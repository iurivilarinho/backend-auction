package com.br.auction.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.br.auction.enums.AuctionProvider;
import com.br.auction.enums.AuctionStatus;
import com.br.auction.enums.LotType;
import com.br.auction.models.Auction;
import com.br.auction.models.AuctionItem;
import com.br.auction.response.AuctionItemResponse;
import com.br.auction.response.AuctionJsonResponse;
import com.br.auction.response.AuctionListJsonResponse;
import com.br.auction.response.AuctionListResponse;
import com.br.auction.response.AuctionResponse;
import com.br.auction.response.HealthResponse;
import com.br.auction.response.ProviderResponse;
import com.br.auction.integration.execution.IntegrationScheduler;
import com.br.auction.response.AuctionItemFacetsResponse;
import com.br.auction.service.AuctionDetranService;
import com.br.auction.service.AuctionItemQuery;
import com.br.auction.service.AuctionService;
import com.br.auction.service.AuctionSourceFilter;
import com.br.auction.service.DistanceService;
import com.br.auction.service.PriceStatGroupBy;
import com.br.auction.response.PriceStatResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "Leiloes", description = "Endpoints relacionados a leiloes e integracao com provedores externos")
public class AuctionController {

	private final AuctionService auctionService;
	private final AuctionDetranService detranService;
	private final IntegrationScheduler integrationScheduler;
	private final DistanceService distanceService;
	private final Environment environment;

	public AuctionController(AuctionService auctionService, AuctionDetranService detranService,
			IntegrationScheduler integrationScheduler, DistanceService distanceService, Environment environment) {
		this.auctionService = auctionService;
		this.detranService = detranService;
		this.integrationScheduler = integrationScheduler;
		this.distanceService = distanceService;
		this.environment = environment;
	}

	@Operation(summary = "Verificar saude da API", description = "Retorna status basico da API, banco atual e provedor padrao.")
	@ApiResponse(responseCode = "200", description = "API disponivel")
	@GetMapping("/health")
	public ResponseEntity<HealthResponse> health() {
		AuctionProvider provider = AuctionProvider.defaultProvider();
		String databaseUrl = environment.getProperty("spring.datasource.url", "indefinido");
		return ResponseEntity.ok(new HealthResponse("ok", databaseUrl, new ProviderResponse(provider, true)));
	}

	@Operation(summary = "Listar provedores", description = "Retorna os provedores de leilao suportados pela API.")
	@ApiResponse(responseCode = "200", description = "Provedores retornados com sucesso")
	@GetMapping("/providers")
	public ResponseEntity<List<ProviderResponse>> findProviders() {
		AuctionProvider defaultProvider = AuctionProvider.defaultProvider();
		List<ProviderResponse> providers = Arrays.stream(AuctionProvider.values())
				.map(provider -> new ProviderResponse(provider, provider == defaultProvider))
				.toList();
		return ResponseEntity.ok(providers);
	}

	@Operation(summary = "Atualizar fonte (via modulo de integracao)", description = "Dispara agora as integracoes agendadas do provedor (coleta de leiloes e lotes). Toda informacao do provedor passa pelo modulo de integracao; os dados sao atualizados de forma incremental em segundo plano.")
	@ApiResponse(responseCode = "202", description = "Coleta iniciada")
	@PostMapping("/auctions/sync")
	public ResponseEntity<java.util.Map<String, Object>> syncAuctions() {
		int started = integrationScheduler.triggerNow();
		java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
		body.put("started", started > 0);
		body.put("integrations", started);
		body.put("message", started > 0
				? "Coleta do provedor iniciada via modulo de integracao. Os dados serao atualizados em instantes."
				: "Nenhuma integracao agendada ativa encontrada.");
		return ResponseEntity.accepted().body(body);
	}

	@Operation(summary = "Listar leiloes", description = "Retorna uma lista paginada de leiloes persistidos no backend.")
	@ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
	@GetMapping("/auctions")
	public ResponseEntity<Page<AuctionListResponse>> findAll(
			@Parameter(description = "Status normalizado") @RequestParam(required = false) List<AuctionStatus> status,
			@Parameter(description = "Busca textual") @RequestParam(required = false) String search,
			@Parameter(description = "Codigos dos provedores (um ou mais)") @RequestParam(required = false) List<String> providerCode,
			@Parameter(description = "Codigo do estado") @RequestParam(required = false) String stateCode,
			Pageable pageable) {
		Page<Auction> auctions = auctionService.findAll(status, search, providerCode, stateCode, pageable);
		Page<AuctionListResponse> responses = auctions.map(AuctionListResponse::new);
		java.util.Map<String, Double> distancesByCity = new java.util.HashMap<>();
		for (AuctionListResponse auction : responses.getContent()) {
			if (auction.getCity() == null || auction.getCity().isBlank()) {
				continue;
			}
			String key = auction.getCity() + "|" + (auction.getStateCode() == null ? "" : auction.getStateCode());
			auction.setDistanceKm(distancesByCity.computeIfAbsent(key,
					k -> distanceService.distanceKm(auction.getCity(), auction.getStateCode())));
		}
		return ResponseEntity.ok(responses);
	}

	@Operation(summary = "Buscar leilao por ID", description = "Retorna um leilao persistido com seus itens.")
	@ApiResponse(responseCode = "200", description = "Leilao encontrado")
	@ApiResponse(responseCode = "404", description = "Leilao nao encontrado")
	@GetMapping("/auctions/{auctionId}")
	public ResponseEntity<AuctionResponse> findById(@PathVariable Long auctionId) {
		Auction auction = auctionService.findById(auctionId);
		return ResponseEntity.ok(new AuctionResponse(auction));
	}

	@Operation(summary = "Baixar edital do leilao", description = "Devolve o PDF do edital guardado na base; se ainda nao houver, baixa do DETRAN, guarda e devolve.")
	@ApiResponse(responseCode = "200", description = "PDF do edital")
	@ApiResponse(responseCode = "404", description = "Edital nao disponivel para este leilao")
	@GetMapping("/auctions/{auctionId}/edital/download")
	public ResponseEntity<byte[]> downloadEdital(@PathVariable Long auctionId) {
		Auction auction = auctionService.getOrFetchEdital(auctionId);
		if (auction == null) {
			return ResponseEntity.notFound().build();
		}
		String fileName = auction.getEditalFileName() != null ? auction.getEditalFileName()
				: "edital-" + auctionId + ".pdf";
		MediaType mediaType = auction.getEditalContentType() == null ? MediaType.APPLICATION_PDF
				: MediaType.parseMediaType(auction.getEditalContentType());
		return ResponseEntity.ok()
				.header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
						"inline; filename=\"" + fileName + "\"")
				.contentType(mediaType)
				.body(auction.getEditalBytes());
	}

	@Operation(summary = "Listar itens de leilao", description = "Retorna itens de leilao persistidos no backend, com filtros especializados (marca, ano, modelo, faixas de lance/FIPE) alem da busca textual.")
	@ApiResponse(responseCode = "200", description = "Itens retornados com sucesso")
	@GetMapping("/auction-items")
	public ResponseEntity<Page<AuctionItemResponse>> findAllItems(
			@Parameter(description = "ID do leilao") @RequestParam(required = false) Long auctionId,
			@Parameter(description = "Status do leilao") @RequestParam(required = false) List<AuctionStatus> auctionStatus,
			@Parameter(description = "Tipo do lote") @RequestParam(required = false) List<LotType> type,
			@Parameter(description = "Busca textual") @RequestParam(required = false) String search,
			@Parameter(description = "Marcas (uma ou mais)") @RequestParam(required = false) List<String> brand,
			@Parameter(description = "Anos (um ou mais)") @RequestParam(required = false) List<String> year,
			@Parameter(description = "Modelo (contem)") @RequestParam(required = false) String model,
			@Parameter(description = "Valor minimo do lance") @RequestParam(required = false) BigDecimal minBid,
			@Parameter(description = "Valor maximo do lance") @RequestParam(required = false) BigDecimal maxBid,
			@Parameter(description = "Valor FIPE minimo") @RequestParam(required = false) BigDecimal minFipe,
			@Parameter(description = "Valor FIPE maximo") @RequestParam(required = false) BigDecimal maxFipe,
			@Parameter(description = "Filtra por lances encerrados (true) ou nao encerrados (false)") @RequestParam(required = false) Boolean closed,
			@Parameter(description = "Codigos dos provedores (um ou mais)") @RequestParam(required = false) List<String> providerCode,
			@Parameter(description = "Codigo do estado") @RequestParam(required = false) String stateCode,
			Pageable pageable) {
		AuctionItemQuery query = new AuctionItemQuery(auctionId, auctionStatus, type, search, brand, year, model,
				minBid, maxBid, minFipe, maxFipe, closed, providerCode, stateCode);
		Page<AuctionItem> items = auctionService.findAllItems(query, pageable);
		Page<AuctionItemResponse> responses = items.map(AuctionItemResponse::new);
		enrichDistances(responses.getContent());
		return ResponseEntity.ok(responses);
	}

	/**
	 * Preenche a distancia de cada item a partir da origem configurada, calculando uma unica vez por
	 * cidade distinta. Cidades ainda nao geocodificadas ficam com distancia nula e sao resolvidas em
	 * segundo plano, sem bloquear a listagem.
	 */
	private void enrichDistances(List<AuctionItemResponse> responses) {
		java.util.Map<String, Double> distancesByCity = new java.util.HashMap<>();
		for (AuctionItemResponse response : responses) {
			AuctionListResponse auction = response.getAuction();
			if (auction == null || auction.getCity() == null || auction.getCity().isBlank()) {
				continue;
			}
			String city = auction.getCity();
			String state = auction.getStateCode();
			String key = city + "|" + (state == null ? "" : state);
			Double distance = distancesByCity.computeIfAbsent(key, k -> distanceService.distanceKm(city, state));
			response.setDistanceKm(distance);
		}
	}

	@Operation(summary = "Buscar FIPE de um item", description = "Calcula e persiste o valor da tabela FIPE de um item sob demanda (usa cache de 30 dias).")
	@ApiResponse(responseCode = "200", description = "Item atualizado com o valor FIPE")
	@ApiResponse(responseCode = "404", description = "Item nao encontrado")
	@PostMapping("/auction-items/{itemId}/fipe")
	public ResponseEntity<AuctionItemResponse> enrichItemFipe(@PathVariable Long itemId) {
		AuctionItemResponse response = new AuctionItemResponse(auctionService.enrichItemFipe(itemId));
		enrichDistances(List.of(response));
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Buscar FIPE de todos os itens de um leilao", description = "Dispara em segundo plano a busca FIPE dos itens do leilao que ainda nao tem valor, respeitando o cache.")
	@ApiResponse(responseCode = "202", description = "Enriquecimento iniciado")
	@PostMapping("/auctions/{auctionId}/items/fipe")
	public ResponseEntity<java.util.Map<String, Object>> enrichAuctionFipe(@PathVariable Long auctionId) {
		auctionService.enrichAuctionItemsFipeAsync(auctionId);
		java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
		body.put("started", true);
		body.put("message", "Busca FIPE iniciada em segundo plano. Os valores aparecerao em instantes.");
		return ResponseEntity.accepted().body(body);
	}

	@Operation(summary = "Precos medios de arremate", description = "Estatisticas (media, minimo, maximo, quantidade) do valor de arremate sobre veiculos com lances ENCERRADOS, agrupadas por marca / marca+modelo / marca+modelo+ano.")
	@ApiResponse(responseCode = "200", description = "Estatisticas retornadas com sucesso")
	@GetMapping("/auction-items/price-stats")
	public ResponseEntity<List<PriceStatResponse>> priceStats(
			@Parameter(description = "Granularidade do agrupamento") @RequestParam(defaultValue = "BRAND_MODEL_YEAR") PriceStatGroupBy groupBy,
			@Parameter(description = "Filtrar por uma ou mais marcas") @RequestParam(required = false) List<String> brand,
			@Parameter(description = "Codigos dos provedores (um ou mais)") @RequestParam(required = false) List<String> providerCode,
			@Parameter(description = "Codigo do estado") @RequestParam(required = false) String stateCode) {
		return ResponseEntity.ok(auctionService.priceStats(groupBy, brand, providerCode, stateCode));
	}

	@Operation(summary = "Facetas de filtro de itens", description = "Retorna as marcas e anos distintos disponiveis para montar os filtros especializados, respeitando o escopo (leilao, provedor, estado).")
	@ApiResponse(responseCode = "200", description = "Facetas retornadas com sucesso")
	@GetMapping("/auction-items/facets")
	public ResponseEntity<AuctionItemFacetsResponse> findItemFacets(
			@Parameter(description = "ID do leilao") @RequestParam(required = false) Long auctionId,
			@Parameter(description = "Codigos dos provedores (um ou mais)") @RequestParam(required = false) List<String> providerCode,
			@Parameter(description = "Codigo do estado") @RequestParam(required = false) String stateCode) {
		List<String> brands = auctionService.distinctBrands(auctionId, providerCode, stateCode);
		List<String> years = auctionService.distinctYears(auctionId, providerCode, stateCode);
		return ResponseEntity.ok(new AuctionItemFacetsResponse(brands, years));
	}

	@Operation(summary = "Detalhes de lotes do provedor", description = "Retorna dados convertidos diretamente do provedor externo. Nao sao dados persistidos.")
	@ApiResponse(responseCode = "200", description = "Dados retornados com sucesso")
	@GetMapping("/json/site/details/auction")
	public ResponseEntity<AuctionJsonResponse> getAuctionLots(
			@RequestParam(required = false, defaultValue = "DETRAN_MG") String providerCode,
			@RequestParam String auctionId,
			@RequestParam String auctionYear) throws IOException {
		AuctionProvider provider = AuctionProvider.fromCodeOrDefault(providerCode);
		return ResponseEntity.ok(detranService.fetchAuctionLots(provider, auctionId, auctionYear));
	}

	@Operation(summary = "Listar leiloes do provedor", description = "Retorna leiloes obtidos diretamente do provedor externo, sem persistencia.")
	@ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
	@GetMapping("/json/site/auctions")
	public ResponseEntity<List<AuctionListJsonResponse>> getAuctions(
			@RequestParam(required = false, defaultValue = "DETRAN_MG") String providerCode) throws IOException {
		AuctionProvider provider = AuctionProvider.fromCodeOrDefault(providerCode);
		return ResponseEntity.ok(detranService.fetchAuctions(provider, new AuctionSourceFilter()));
	}
}
