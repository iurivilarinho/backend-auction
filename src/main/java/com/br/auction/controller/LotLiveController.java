package com.br.auction.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.br.auction.service.LotLiveRefreshService;
import com.br.auction.service.LotLiveRefreshService.RefreshResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Dispara a atualizacao AO VIVO dos lotes (lance atual real, prazo por lote e status), buscando lote
 * a lote no provedor. Throttle padrao para nao martelar a fonte.
 */
@RestController
@RequestMapping("/api/lots")
@Tag(name = "Lotes - Dados ao vivo", description = "Atualiza lance atual, prazo por lote e status direto do provedor")
public class LotLiveController {

	private final LotLiveRefreshService service;

	public LotLiveController(LotLiveRefreshService service) {
		this.service = service;
	}

	@Operation(summary = "Atualizar dados ao vivo dos lotes de um leilao")
	@PostMapping("/refresh-live/auction/{auctionId}")
	public ResponseEntity<RefreshResult> refreshAuction(@PathVariable Long auctionId,
			@RequestParam(name = "throttleMs", defaultValue = "300") long throttleMs) {
		return ResponseEntity.ok(service.refreshAuction(auctionId, throttleMs));
	}

	@Operation(summary = "Atualizar dados ao vivo de TODOS os lotes (backfill, pode demorar)")
	@PostMapping("/refresh-live/all")
	public ResponseEntity<RefreshResult> refreshAll(
			@RequestParam(name = "throttleMs", defaultValue = "400") long throttleMs) {
		return ResponseEntity.ok(service.refreshAll(throttleMs));
	}
}
