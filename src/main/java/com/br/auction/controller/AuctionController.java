package com.br.auction.controller;

import java.io.IOException;
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
import com.br.auction.models.Auction;
import com.br.auction.models.AuctionItem;
import com.br.auction.response.AuctionItemResponse;
import com.br.auction.response.AuctionJsonResponse;
import com.br.auction.response.AuctionListJsonResponse;
import com.br.auction.response.AuctionListResponse;
import com.br.auction.response.AuctionResponse;
import com.br.auction.response.FipeEnrichStartedResponse;
import com.br.auction.response.FipeEnrichStatusResponse;
import com.br.auction.response.HealthResponse;
import com.br.auction.response.IntegrationTriggerResponse;
import com.br.auction.response.ProviderResponse;
import com.br.auction.integration.execution.IntegrationScheduler;
import com.br.auction.response.AuctionItemFacetsResponse;
import com.br.auction.filter.AuctionFilter;
import com.br.auction.filter.AuctionItemFacetFilter;
import com.br.auction.filter.AuctionItemFilter;
import com.br.auction.filter.AuctionSourceFilter;
import com.br.auction.filter.PriceStatFilter;
import com.br.auction.service.AuctionDetranService;
import com.br.auction.service.AuctionService;
import com.br.auction.response.PriceStatResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "Leiloes", description = "Endpoints relacionados a leiloes e integracao com provedores externos")
public class AuctionController {

	private final AuctionService auctionService;
	private final AuctionDetranService detranService;
	private final IntegrationScheduler integrationScheduler;
	private final Environment environment;

	public AuctionController(AuctionService auctionService, AuctionDetranService detranService,
			IntegrationScheduler integrationScheduler, Environment environment) {
		this.auctionService = auctionService;
		this.detranService = detranService;
		this.integrationScheduler = integrationScheduler;
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
	public ResponseEntity<IntegrationTriggerResponse> syncAuctions() {
		int started = integrationScheduler.triggerNow();
		String message = started > 0
				? "Coleta do provedor iniciada via modulo de integracao. Os dados serao atualizados em instantes."
				: "Nenhuma integracao agendada ativa encontrada.";
		return ResponseEntity.accepted().body(new IntegrationTriggerResponse(started, message));
	}

	@Operation(summary = "Listar leiloes", description = "Retorna uma lista paginada de leiloes persistidos no backend.")
	@ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
	@GetMapping("/auctions")
	public ResponseEntity<Page<AuctionListResponse>> findAll(AuctionFilter filter, Pageable pageable) {
		Page<Auction> auctions = auctionService.findAll(filter, pageable);
		Page<AuctionListResponse> responses = auctions.map(AuctionListResponse::new);
		auctionService.fillAuctionDistances(responses.getContent());
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
	public ResponseEntity<Page<AuctionItemResponse>> findAllItems(AuctionItemFilter filter, Pageable pageable) {
		Page<AuctionItem> items = auctionService.findAllItems(filter, pageable);
		Page<AuctionItemResponse> responses = items.map(AuctionItemResponse::new);
		auctionService.fillItemDistances(responses.getContent());
		return ResponseEntity.ok(responses);
	}

	@Operation(summary = "Buscar FIPE de um item", description = "Calcula e persiste o valor da tabela FIPE de um item sob demanda (usa cache de 30 dias).")
	@ApiResponse(responseCode = "200", description = "Item atualizado com o valor FIPE")
	@ApiResponse(responseCode = "404", description = "Item nao encontrado")
	@PostMapping("/auction-items/{itemId}/fipe")
	public ResponseEntity<AuctionItemResponse> enrichItemFipe(@PathVariable Long itemId) {
		AuctionItemResponse response = new AuctionItemResponse(auctionService.enrichItemFipe(itemId));
		auctionService.fillItemDistances(List.of(response));
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Buscar FIPE de todos os itens de um leilao", description = "Dispara em segundo plano a busca FIPE dos itens do leilao que ainda nao tem valor, respeitando o cache.")
	@ApiResponse(responseCode = "202", description = "Enriquecimento iniciado")
	@PostMapping("/auctions/{auctionId}/items/fipe")
	public ResponseEntity<FipeEnrichStartedResponse> enrichAuctionFipe(@PathVariable Long auctionId) {
		boolean started = auctionService.startAuctionFipeEnrichment(auctionId);
		String message = started ? "Busca FIPE iniciada em segundo plano. Os valores aparecerao em instantes."
				: "Ja existe uma busca FIPE em andamento para este leilao.";
		return ResponseEntity.accepted().body(new FipeEnrichStartedResponse(started, message));
	}

	@Operation(summary = "Situacao da busca FIPE de um leilao", description = "Retorna se a busca FIPE em segundo plano ainda esta em andamento e o progresso, permitindo a tela reidratar apos um refresh.")
	@ApiResponse(responseCode = "200", description = "Situacao retornada com sucesso")
	@GetMapping("/auctions/{auctionId}/items/fipe/status")
	public ResponseEntity<FipeEnrichStatusResponse> auctionFipeStatus(@PathVariable Long auctionId) {
		return ResponseEntity.ok(auctionService.auctionFipeStatus(auctionId));
	}

	@Operation(summary = "Precos medios de arremate", description = "Estatisticas (media, minimo, maximo, quantidade) do valor de arremate sobre veiculos com lances ENCERRADOS, agrupadas por marca / marca+modelo / marca+modelo+ano.")
	@ApiResponse(responseCode = "200", description = "Estatisticas retornadas com sucesso")
	@GetMapping("/auction-items/price-stats")
	public ResponseEntity<List<PriceStatResponse>> priceStats(PriceStatFilter filter) {
		return ResponseEntity.ok(auctionService.priceStats(filter));
	}

	@Operation(summary = "Facetas de filtro de itens", description = "Retorna as marcas e anos distintos disponiveis para montar os filtros especializados, respeitando o escopo (leilao, provedor, estado).")
	@ApiResponse(responseCode = "200", description = "Facetas retornadas com sucesso")
	@GetMapping("/auction-items/facets")
	public ResponseEntity<AuctionItemFacetsResponse> findItemFacets(AuctionItemFacetFilter filter) {
		return ResponseEntity.ok(auctionService.findItemFacets(filter));
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
