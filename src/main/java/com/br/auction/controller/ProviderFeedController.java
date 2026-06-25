package com.br.auction.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.br.auction.enums.AuctionProvider;
import com.br.auction.response.AuctionJsonResponse;
import com.br.auction.response.AuctionListJsonResponse;
import com.br.auction.response.LotResponse;
import com.br.auction.service.AuctionDetranService;
import com.br.auction.service.AuctionSourceFilter;

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

	public ProviderFeedController(AuctionDetranService detranService) {
		this.detranService = detranService;
	}

	@Operation(summary = "Leiloes reais do provedor (JSON paginado)")
	@ApiResponse(responseCode = "200", description = "Pagina retornada")
	@GetMapping("/auctions")
	public ResponseEntity<Map<String, Object>> auctions(
			@RequestParam(required = false, defaultValue = "DETRAN_MG") String providerCode,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "100") int pageSize) throws IOException {
		AuctionProvider provider = AuctionProvider.fromCodeOrDefault(providerCode);
		List<AuctionListJsonResponse> auctions = detranService.fetchAuctions(provider, new AuctionSourceFilter());

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

		String resolvedAuctionId = auctionId;
		String resolvedYear = auctionYear;
		if (resolvedAuctionId == null || resolvedAuctionId.isBlank()) {
			List<AuctionListJsonResponse> auctions = detranService.fetchAuctions(provider, new AuctionSourceFilter());
			if (auctions.isEmpty()) {
				return ResponseEntity.ok(page(List.of(), false, page, pageSize, "lots"));
			}
			resolvedAuctionId = auctions.get(0).getAuctionId();
			resolvedYear = auctions.get(0).getAuctionYear();
		}

		AuctionJsonResponse details = detranService.fetchAuctionLots(provider, resolvedAuctionId, resolvedYear);
		List<LotResponse> lots = details.getLots();
		int from = Math.max(0, (page - 1) * pageSize);
		int to = Math.min(lots.size(), from + pageSize);
		List<Map<String, Object>> items = new ArrayList<>();
		for (LotResponse lot : from >= lots.size() ? List.<LotResponse>of() : lots.subList(from, to)) {
			items.add(lotToMap(lot, resolvedAuctionId));
		}
		return ResponseEntity.ok(page(items, to < lots.size(), page, pageSize, "lots"));
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
