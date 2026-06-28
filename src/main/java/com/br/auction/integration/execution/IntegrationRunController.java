package com.br.auction.integration.execution;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/integration")
@Tag(name = "Integracao - Execucoes", description = "Historico e detalhe das execucoes de integracao")
public class IntegrationRunController {

	private final IntegrationRunService service;

	public IntegrationRunController(IntegrationRunService service) {
		this.service = service;
	}

	@Operation(summary = "Listar execucoes de uma integracao")
	@ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
	@GetMapping("/integrations/{integrationId}/runs")
	public ResponseEntity<Page<IntegrationRunResponse>> findByIntegration(@PathVariable Long integrationId,
			Pageable pageable) {
		return ResponseEntity.ok(service.findByIntegration(integrationId, pageable));
	}

	@Operation(summary = "Listar todas as execucoes", description = "Lista paginada de execucoes de todas as integracoes (mais recentes primeiro).")
	@ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
	@GetMapping("/runs")
	public ResponseEntity<Page<IntegrationRunResponse>> findAllRuns(Pageable pageable) {
		return ResponseEntity.ok(service.findAllRuns(pageable));
	}

	@Operation(summary = "Resumo das execucoes por status", description = "Retorna a contagem de execucoes agrupadas por status.")
	@ApiResponse(responseCode = "200", description = "Resumo retornado com sucesso")
	@GetMapping("/runs/summary")
	public ResponseEntity<java.util.Map<String, Long>> runSummary() {
		return ResponseEntity.ok(service.runSummary());
	}

	@Operation(summary = "Listar execucoes em andamento", description = "Retorna todas as execucoes com status RUNNING (para indicar integracoes em andamento na lista).")
	@ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
	@GetMapping("/runs/running")
	public ResponseEntity<java.util.List<IntegrationRunResponse>> findRunning() {
		return ResponseEntity.ok(service.findRunningRuns());
	}

	@Operation(summary = "Buscar execucao por ID")
	@ApiResponse(responseCode = "200", description = "Execucao encontrada")
	@ApiResponse(responseCode = "404", description = "Execucao nao encontrada")
	@GetMapping("/runs/{runId}")
	public ResponseEntity<IntegrationRunResponse> findRun(@PathVariable Long runId) {
		return ResponseEntity.ok(service.findRun(runId));
	}

	@Operation(summary = "Listar itens processados de uma execucao")
	@ApiResponse(responseCode = "200", description = "Itens retornados com sucesso")
	@GetMapping("/runs/{runId}/items")
	public ResponseEntity<Page<IntegrationItemLogResponse>> findRunItems(@PathVariable Long runId, Pageable pageable) {
		return ResponseEntity.ok(service.findRunItems(runId, pageable));
	}
}
