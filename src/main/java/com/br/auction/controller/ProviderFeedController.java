package com.br.auction.controller;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.br.auction.response.AuctionFeedPageResponse;
import com.br.auction.response.LotFeedPageResponse;
import com.br.auction.response.LotLiveFeedPageResponse;
import com.br.auction.service.ProviderFeedService;

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

	private final ProviderFeedService providerFeedService;

	public ProviderFeedController(ProviderFeedService providerFeedService) {
		this.providerFeedService = providerFeedService;
	}

	@Operation(summary = "Leiloes reais do provedor (JSON paginado)")
	@ApiResponse(responseCode = "200", description = "Pagina retornada")
	@GetMapping("/auctions")
	public ResponseEntity<AuctionFeedPageResponse> auctions(
			@RequestParam(required = false, defaultValue = "DETRAN_MG") String providerCode,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "100") int pageSize) throws IOException {
		return ResponseEntity.ok(providerFeedService.auctions(providerCode, page, pageSize));
	}

	@Operation(summary = "Lotes reais do provedor (JSON paginado)", description = "Lotes do leilao informado, ou do leilao mais recente quando nao informado.")
	@ApiResponse(responseCode = "200", description = "Pagina retornada")
	@GetMapping("/lots")
	public ResponseEntity<LotFeedPageResponse> lots(
			@RequestParam(required = false, defaultValue = "DETRAN_MG") String providerCode,
			@RequestParam(required = false) String auctionId,
			@RequestParam(required = false) String auctionYear,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "200") int pageSize) throws IOException {
		return ResponseEntity.ok(providerFeedService.lots(providerCode, auctionId, auctionYear, page, pageSize));
	}

	@Operation(summary = "Dados AO VIVO dos lotes (lance real, prazo por lote e status)",
			description = "Adapter do provedor: busca lote a lote no endpoint do cronometro. Prioriza lotes abertos "
					+ "(sem prazo/novos primeiro, depois os que encerram antes), limitado por rodada. Paginado.")
	@ApiResponse(responseCode = "200", description = "Pagina retornada")
	@GetMapping("/lots-live")
	public ResponseEntity<LotLiveFeedPageResponse> lotsLive(
			@RequestParam(required = false, defaultValue = "DETRAN_MG") String providerCode,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "50") int pageSize) {
		return ResponseEntity.ok(providerFeedService.lotsLive(providerCode, page, pageSize));
	}
}
