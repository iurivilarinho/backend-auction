package com.br.auction.integration.execution;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Recebe dados empurrados por uma fonte externa (inbound) e os integra aos modelos
 * internos da aplicacao, aplicando o de->para da integracao identificada pelo codigo.
 */
@RestController
@RequestMapping("/api/integration/inbound")
@Tag(name = "Integracao - Recebimento", description = "Recebimento de dados via push de fontes externas")
public class InboundController {

	private final IntegrationRunService service;

	public InboundController(IntegrationRunService service) {
		this.service = service;
	}

	@Operation(summary = "Receber dados de uma fonte", description = "Aceita um objeto JSON ou um array de objetos e os integra ao modelo interno de destino.")
	@ApiResponse(responseCode = "200", description = "Recebimento processado")
	@ApiResponse(responseCode = "404", description = "Integracao nao encontrada")
	@PostMapping("/{code}")
	public ResponseEntity<IntegrationRunResponse> receive(@PathVariable String code, @RequestBody Object body) {
		return ResponseEntity.ok(new IntegrationRunResponse(service.receiveInbound(code, body)));
	}
}
