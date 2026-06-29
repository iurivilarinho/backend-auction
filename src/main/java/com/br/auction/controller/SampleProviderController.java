package com.br.auction.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.br.auction.response.AuctionFeedPageResponse;
import com.br.auction.service.SampleProviderService;

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

	private final SampleProviderService sampleProviderService;

	public SampleProviderController(SampleProviderService sampleProviderService) {
		this.sampleProviderService = sampleProviderService;
	}

	@Operation(summary = "Leiloes do provedor de exemplo", description = "Retorna leiloes em paginas no formato {items, hasNext}.")
	@ApiResponse(responseCode = "200", description = "Pagina retornada")
	@GetMapping("/auctions")
	public ResponseEntity<AuctionFeedPageResponse> auctions(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "4") int pageSize) {
		return ResponseEntity.ok(sampleProviderService.auctions(page, pageSize));
	}
}
