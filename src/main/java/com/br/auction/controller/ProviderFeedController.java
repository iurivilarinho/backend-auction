package com.br.auction.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.br.auction.enums.AuctionProvider;
import com.br.auction.models.AuctionItem;
import com.br.auction.repository.AuctionItemRepository;
import com.br.auction.response.AuctionJsonResponse;
import com.br.auction.response.AuctionListJsonResponse;
import com.br.auction.response.LotResponse;
import com.br.auction.service.AuctionDetranService;
import com.br.auction.service.AuctionDetranService.LotLiveData;
import com.br.auction.service.AuctionSourceFilter;
import com.br.auction.service.LeiloService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Expoe os dados coletados do provedor (scraping real do DETRAN-MG) no formato JSON paginado
 * {@code {items|lots, hasNext}}. Serve para o modulo de integracao consumir o provedor pelo
 * proprio backend, transformando o scraping em uma fonte REST consumivel pela integracao.
 */
@RestController
@RequestMapping("/api/feed")
@Tag(name = "Feed do provedor", description = "Dados reais do provedor (DETRAN-MG) em JSON paginado para as integracoes")
public class ProviderFeedController {

	private final AuctionDetranService detranService;
	private final LeiloService leiloService;
	private final AuctionItemRepository auctionItemRepository;
	private final int maxAuctionsForLots;
	private final int liveMaxPerRun;
	private final long liveThrottleMs;

	public ProviderFeedController(AuctionDetranService detranService, LeiloService leiloService,
			AuctionItemRepository auctionItemRepository,
			@Value("${auction.feed.max-auctions:3}") int maxAuctionsForLots,
			@Value("${lot.live.max-per-run:300}") int liveMaxPerRun,
			@Value("${lot.live.throttle-ms:200}") long liveThrottleMs) {
		this.detranService = detranService;
		this.leiloService = leiloService;
		this.auctionItemRepository = auctionItemRepository;
		this.maxAuctionsForLots = maxAuctionsForLots;
		this.liveMaxPerRun = liveMaxPerRun;
		this.liveThrottleMs = liveThrottleMs;
	}

	@Operation(summary = "Leiloes reais do provedor (JSON paginado)")
	@ApiResponse(responseCode = "200", description = "Pagina retornada")
	@GetMapping("/auctions")
	public ResponseEntity<Map<String, Object>> auctions(
			@RequestParam(required = false, defaultValue = "DETRAN_MG") String providerCode,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "100") int pageSize) throws IOException {
		AuctionProvider provider = AuctionProvider.fromCodeOrDefault(providerCode);
		List<AuctionListJsonResponse> auctions = provider == AuctionProvider.LEILO_GO
				? leiloService.fetchAuctions(provider)
				: detranService.fetchAuctions(provider, new AuctionSourceFilter());

		int from = Math.max(0, (page - 1) * pageSize);
		int to = Math.min(auctions.size(), from + pageSize);
		List<Map<String, Object>> items = new ArrayList<>();
		for (AuctionListJsonResponse auction : from >= auctions.size() ? List.<AuctionListJsonResponse>of()
				: auctions.subList(from, to)) {
			items.add(auctionToMap(auction));
		}
		return ResponseEntity.ok(page(items, to < auctions.size(), page, pageSize, "items"));
	}

	@Operation(summary = "Lotes reais do provedor (JSON paginado)", description = "Lotes do leilao informado, ou do leilao mais recente quando nao informado.")
	@ApiResponse(responseCode = "200", description = "Pagina retornada")
	@GetMapping("/lots")
	public ResponseEntity<Map<String, Object>> lots(
			@RequestParam(required = false, defaultValue = "DETRAN_MG") String providerCode,
			@RequestParam(required = false) String auctionId,
			@RequestParam(required = false) String auctionYear,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "200") int pageSize) throws IOException {
		AuctionProvider provider = AuctionProvider.fromCodeOrDefault(providerCode);

		// LEILO: lotes vem da API publica (busca-elastic), ja filtrados (veiculos/orgao) e paginados.
		if (provider == AuctionProvider.LEILO_GO) {
			LeiloService.LotsPage lotsPage = leiloService.fetchLotsPage(provider, page, pageSize);
			return ResponseEntity.ok(page(lotsPage.lots(), lotsPage.hasNext(), page, pageSize, "lots"));
		}

		// Lotes ja achatados (com o id do leilao pai) de um ou mais leiloes.
		List<Map<String, Object>> allLots = new ArrayList<>();
		if (auctionId != null && !auctionId.isBlank()) {
			collectLots(provider, auctionId, auctionYear, allLots);
		} else {
			List<AuctionListJsonResponse> auctions = detranService.fetchAuctions(provider, new AuctionSourceFilter());
			int limit = maxAuctionsForLots <= 0 ? auctions.size() : Math.min(maxAuctionsForLots, auctions.size());
			for (int i = 0; i < limit; i++) {
				AuctionListJsonResponse auction = auctions.get(i);
				collectLots(provider, auction.getAuctionId(), auction.getAuctionYear(), allLots);
			}
		}

		int from = Math.max(0, (page - 1) * pageSize);
		int to = Math.min(allLots.size(), from + pageSize);
		List<Map<String, Object>> items = from >= allLots.size() ? List.of() : allLots.subList(from, to);
		return ResponseEntity.ok(page(items, to < allLots.size(), page, pageSize, "lots"));
	}

	@Operation(summary = "Dados AO VIVO dos lotes (lance real, prazo por lote e status)",
			description = "Adapter do provedor: busca lote a lote no endpoint do cronometro. Prioriza lotes abertos "
					+ "(sem prazo/novos primeiro, depois os que encerram antes), limitado por rodada. Paginado.")
	@ApiResponse(responseCode = "200", description = "Pagina retornada")
	@GetMapping("/lots-live")
	public ResponseEntity<Map<String, Object>> lotsLive(
			@RequestParam(required = false, defaultValue = "DETRAN_MG") String providerCode,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "50") int pageSize) {
		AuctionProvider provider = AuctionProvider.fromCodeOrDefault(providerCode);

		// Lotes abertos priorizados: sem prazo (novos) primeiro, depois os que encerram mais cedo.
		List<AuctionItem> open = auctionItemRepository.findAll().stream()
				.filter(ProviderFeedController::isOpen)
				.sorted(Comparator.comparing(AuctionItem::getLotClosingDate,
						Comparator.nullsFirst(Comparator.naturalOrder())))
				.toList();

		int cap = Math.min(open.size(), Math.max(0, liveMaxPerRun));
		int from = Math.max(0, (page - 1) * pageSize);
		int to = Math.min(cap, from + pageSize);

		List<Map<String, Object>> lots = new ArrayList<>();
		for (int i = from; i < to; i++) {
			AuctionItem item = open.get(i);
			Optional<LotLiveData> live = detranService.fetchLotLive(provider, item.getLotId());
			if (live.isPresent()) {
				lots.add(liveLotToMap(item, live.get()));
			}
			pace(liveThrottleMs);
		}
		return ResponseEntity.ok(page(lots, to < cap, page, pageSize, "lots"));
	}

	/** Lote aberto (aceita lance): status desconhecido ou diferente de encerrado (3/4). */
	private static boolean isOpen(AuctionItem item) {
		String status = item.getLotStatus();
		return status == null || !(status.equals("3") || status.equals("4"));
	}

	private Map<String, Object> liveLotToMap(AuctionItem item, LotLiveData data) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("auctionId", item.getAuction() == null ? null : item.getAuction().getDetranAuctionId());
		map.put("lotId", item.getLotId());
		// Lance ao vivo; sem lance (valor nulo), mostra o piso/inicial para a UI nao ficar zerada.
		BigDecimal bid = data.getCurrentBid();
		if (bid == null) {
			bid = item.getMinimumBidValue() != null ? item.getMinimumBidValue() : item.getCurrentBidValue();
		}
		map.put("currentBidValue", bid == null ? null : bid.toPlainString());
		LocalDateTime closing = data.closingDateFrom(LocalDateTime.now());
		map.put("closingDate", closing == null ? null : closing.toString());
		map.put("lotStatus", data.getStatusLeilao());
		return map;
	}

	private void pace(long throttleMs) {
		if (throttleMs <= 0) {
			return;
		}
		try {
			Thread.sleep(Duration.ofMillis(throttleMs).toMillis());
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	private void collectLots(AuctionProvider provider, String auctionId, String auctionYear,
			List<Map<String, Object>> target) throws IOException {
		AuctionJsonResponse details = detranService.fetchAuctionLots(provider, auctionId, auctionYear);
		for (LotResponse lot : details.getLots()) {
			target.add(lotToMap(lot, auctionId));
		}
	}

	private Map<String, Object> auctionToMap(AuctionListJsonResponse auction) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("auctionId", auction.getAuctionId());
		map.put("auctionNoticeNumber", auction.getAuctionNoticeNumber());
		map.put("city", auction.getCity());
		map.put("auctioneer", auction.getAuctioneer());
		map.put("status", auction.getStatus());
		map.put("closingDate", auction.getClosingDate());
		map.put("auctionYear", auction.getAuctionYear());
		map.put("sourceUrl", auction.getSourceUrl());
		return map;
	}

	private Map<String, Object> lotToMap(LotResponse lot, String auctionId) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("auctionId", auctionId);
		map.put("lotId", lot.getLotId());
		map.put("lotNumber", lot.getLotNumber());
		map.put("lotType", lot.getLotType());
		map.put("vehicleDescription", lot.getVehicleDescription());
		map.put("currentBidValue", lot.getCurrentBidValue());
		map.put("imageUrls", lot.getImageUrls());
		return map;
	}

	private Map<String, Object> page(List<Map<String, Object>> items, boolean hasNext, int page, int pageSize,
			String itemsKey) {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put(itemsKey, items);
		response.put("page", page);
		response.put("pageSize", pageSize);
		response.put("hasNext", hasNext);
		return response;
	}
}
