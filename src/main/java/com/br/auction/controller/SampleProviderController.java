package com.br.auction.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Feed REST de exemplo que simula um provedor externo retornando leiloes em JSON paginado
 * ({@code {items, hasNext, page}}). Serve para demonstrar uma integracao do tipo coleta
 * (pull) funcionando de ponta a ponta de forma offline, sem depender do site real.
 */
@RestController
@RequestMapping("/api/sample/provider")
@Tag(name = "Provedor de exemplo", description = "Feed JSON paginado de leiloes para demonstrar a coleta")
public class SampleProviderController {

	private static final int TOTAL_AUCTIONS = 12;

	@Operation(summary = "Leiloes do provedor de exemplo", description = "Retorna leiloes em paginas no formato {items, hasNext}.")
	@ApiResponse(responseCode = "200", description = "Pagina retornada")
	@GetMapping("/auctions")
	public ResponseEntity<Map<String, Object>> auctions(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "4") int pageSize) {
		List<Map<String, Object>> all = buildAuctions();
		int from = Math.max(0, (page - 1) * pageSize);
		int to = Math.min(all.size(), from + pageSize);
		List<Map<String, Object>> items = from >= all.size() ? List.of() : all.subList(from, to);

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("items", items);
		response.put("page", page);
		response.put("pageSize", pageSize);
		response.put("hasNext", to < all.size());
		return ResponseEntity.ok(response);
	}

	private List<Map<String, Object>> buildAuctions() {
		String[] cities = { "Belo Horizonte", "Uberlandia", "Contagem", "Juiz de Fora", "Betim", "Montes Claros" };
		String[] statuses = { "Publicado", "Em Andamento", "Publicado", "Finalizado" };
		List<Map<String, Object>> auctions = new ArrayList<>(TOTAL_AUCTIONS);
		for (int i = 1; i <= TOTAL_AUCTIONS; i++) {
			Map<String, Object> auction = new LinkedHashMap<>();
			auction.put("auctionId", "FEED-" + String.format("%03d", i));
			auction.put("auctionNoticeNumber", String.format("%03d/2026", i));
			auction.put("city", cities[(i - 1) % cities.length]);
			auction.put("auctioneer", "Patio Regional " + (((i - 1) % 3) + 1));
			auction.put("status", statuses[(i - 1) % statuses.length]);
			auction.put("closingDate", String.format("%02d/08/2026 14:00", ((i - 1) % 28) + 1));
			auction.put("auctionYear", "2026");
			auction.put("sourceUrl", "https://leilao.detran.mg.gov.br/feed/" + i);
			auctions.add(auction);
		}
		return auctions;
	}
}
