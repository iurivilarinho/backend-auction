package com.br.auction.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import com.br.auction.response.AuctionSyncResultResponse;
import com.br.auction.response.HealthResponse;
import com.br.auction.response.ProviderResponse;
import com.br.auction.service.AuctionDetranImportService;
import com.br.auction.service.AuctionDetranService;
import com.br.auction.service.AuctionService;
import com.br.auction.service.AuctionSourceFilter;

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
	private final AuctionDetranImportService importService;
	private final Environment environment;

	public AuctionController(AuctionService auctionService, AuctionDetranService detranService,
			AuctionDetranImportService importService, Environment environment) {
		this.auctionService = auctionService;
		this.detranService = detranService;
		this.importService = importService;
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

	@Operation(summary = "Sincronizar leiloes do provedor", description = "Busca leiloes no provedor externo e persiste no banco configurado. Minas Gerais e o provedor padrao atual.")
	@ApiResponse(responseCode = "200", description = "Sincronizacao concluida")
	@PostMapping("/auctions/sync")
	public ResponseEntity<AuctionSyncResultResponse> syncAuctions(
			@Parameter(description = "Codigo do provedor. Padrao: DETRAN_MG") @RequestParam(required = false) String providerCode,
			@Parameter(description = "Numero do edital no filtro do portal") @RequestParam(required = false) String auctionNumber,
			@Parameter(description = "Data inicial de encerramento no formato dd/MM/yyyy") @RequestParam(required = false) String closingDateStart,
			@Parameter(description = "Data final de encerramento no formato dd/MM/yyyy") @RequestParam(required = false) String closingDateEnd,
			@Parameter(description = "ID do municipio no portal") @RequestParam(required = false) String municipalityId,
			@Parameter(description = "Codigo do tipo de veiculo no portal") @RequestParam(required = false) String vehicleTypeCode,
			@Parameter(description = "Marca no portal") @RequestParam(required = false) String brand,
			@Parameter(description = "Modelo no portal") @RequestParam(required = false) String model,
			@Parameter(description = "Ano do veiculo no portal") @RequestParam(required = false) String vehicleYear,
			@Parameter(description = "Cor do veiculo no portal") @RequestParam(required = false) String color,
			@Parameter(description = "Condicao no portal: R para conservado, S para sucata") @RequestParam(required = false) String condition) {
		AuctionProvider provider = AuctionProvider.fromCodeOrDefault(providerCode);
		AuctionSourceFilter filter = new AuctionSourceFilter();
		filter.setAuctionNumber(auctionNumber);
		filter.setClosingDateStart(closingDateStart);
		filter.setClosingDateEnd(closingDateEnd);
		filter.setMunicipalityId(municipalityId);
		filter.setVehicleTypeCode(vehicleTypeCode);
		filter.setBrand(brand);
		filter.setModel(model);
		filter.setVehicleYear(vehicleYear);
		filter.setColor(color);
		filter.setCondition(condition);

		return ResponseEntity.ok(importService.syncAuctions(provider, filter));
	}

	@Operation(summary = "Listar leiloes", description = "Retorna uma lista paginada de leiloes persistidos no backend.")
	@ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
	@GetMapping("/auctions")
	public ResponseEntity<Page<AuctionListResponse>> findAll(
			@Parameter(description = "Status normalizado") @RequestParam(required = false) List<AuctionStatus> status,
			@Parameter(description = "Busca textual") @RequestParam(required = false) String search,
			@Parameter(description = "Codigo do provedor") @RequestParam(required = false) String providerCode,
			@Parameter(description = "Codigo do estado") @RequestParam(required = false) String stateCode,
			Pageable pageable) {
		Page<Auction> auctions = auctionService.findAll(status, search, providerCode, stateCode, pageable);
		return ResponseEntity.ok(auctions.map(AuctionListResponse::new));
	}

	@Operation(summary = "Buscar leilao por ID", description = "Retorna um leilao persistido com seus itens.")
	@ApiResponse(responseCode = "200", description = "Leilao encontrado")
	@ApiResponse(responseCode = "404", description = "Leilao nao encontrado")
	@GetMapping("/auctions/{auctionId}")
	public ResponseEntity<AuctionResponse> findById(@PathVariable Long auctionId) {
		Auction auction = auctionService.findById(auctionId);
		return ResponseEntity.ok(new AuctionResponse(auction));
	}

	@Operation(summary = "Listar itens de leilao", description = "Retorna itens de leilao persistidos no backend.")
	@ApiResponse(responseCode = "200", description = "Itens retornados com sucesso")
	@GetMapping("/auction-items")
	public ResponseEntity<Page<AuctionItemResponse>> findAllItems(
			@Parameter(description = "ID do leilao") @RequestParam(required = false) Long auctionId,
			@Parameter(description = "Status do leilao") @RequestParam(required = false) List<AuctionStatus> auctionStatus,
			@Parameter(description = "Tipo do lote") @RequestParam(required = false) List<LotType> type,
			@Parameter(description = "Busca textual") @RequestParam(required = false) String search,
			@Parameter(description = "Codigo do provedor") @RequestParam(required = false) String providerCode,
			@Parameter(description = "Codigo do estado") @RequestParam(required = false) String stateCode,
			Pageable pageable) {
		Page<AuctionItem> items = auctionService.findAllItems(auctionId, auctionStatus, type, search, providerCode,
				stateCode, pageable);
		return ResponseEntity.ok(items.map(AuctionItemResponse::new));
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
