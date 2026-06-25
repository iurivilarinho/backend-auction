package com.br.auction.integration.integration;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.br.auction.integration.enums.IntegrationStatus;
import com.br.auction.integration.execution.IntegrationRunResponse;
import com.br.auction.integration.execution.IntegrationRunService;
import com.br.auction.integration.target.InternalTargetModel;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/integration/integrations")
@Tag(name = "Integracao - Integracoes", description = "Configuracao e disparo das integracoes com provedores externos")
public class IntegrationController {

	private final IntegrationService service;
	private final IntegrationRunService runService;

	public IntegrationController(IntegrationService service, IntegrationRunService runService) {
		this.service = service;
		this.runService = runService;
	}

	@Operation(summary = "Listar integracoes", description = "Lista paginada de integracoes com busca e filtros.")
	@ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
	@GetMapping
	public ResponseEntity<Page<IntegrationResponse>> findAll(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Long sourceId,
			@RequestParam(required = false) InternalTargetModel targetModel,
			@RequestParam(required = false) IntegrationStatus status,
			Pageable pageable) {
		Page<IntegrationResponse> page = service.findAll(search, sourceId, targetModel, status, pageable)
				.map(IntegrationResponse::new);
		return ResponseEntity.ok(page);
	}

	@Operation(summary = "Buscar integracao por ID")
	@ApiResponse(responseCode = "200", description = "Integracao encontrada")
	@ApiResponse(responseCode = "404", description = "Integracao nao encontrada")
	@GetMapping("/{id}")
	public ResponseEntity<IntegrationResponse> findById(@PathVariable Long id) {
		return ResponseEntity.ok(new IntegrationResponse(service.findById(id)));
	}

	@Operation(summary = "Criar integracao")
	@ApiResponse(responseCode = "201", description = "Integracao criada")
	@PostMapping
	public ResponseEntity<IntegrationResponse> create(@Valid @RequestBody IntegrationRequest request) {
		Integration created = service.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(new IntegrationResponse(created));
	}

	@Operation(summary = "Atualizar integracao")
	@ApiResponse(responseCode = "200", description = "Integracao atualizada")
	@ApiResponse(responseCode = "404", description = "Integracao nao encontrada")
	@PutMapping("/{id}")
	public ResponseEntity<IntegrationResponse> update(@PathVariable Long id,
			@Valid @RequestBody IntegrationRequest request) {
		return ResponseEntity.ok(new IntegrationResponse(service.update(id, request)));
	}

	@Operation(summary = "Alterar status da integracao", description = "Aplica uma transicao de status valida.")
	@ApiResponse(responseCode = "200", description = "Status atualizado")
	@ApiResponse(responseCode = "400", description = "Transicao invalida")
	@PatchMapping("/{id}/status")
	public ResponseEntity<IntegrationResponse> updateStatus(@PathVariable Long id,
			@Valid @RequestBody IntegrationStatusUpdateRequest request) {
		return ResponseEntity.ok(new IntegrationResponse(service.updateStatus(id, request.getStatus())));
	}

	@Operation(summary = "Executar integracao agora", description = "Dispara manualmente a coleta e gravacao nos modelos internos.")
	@ApiResponse(responseCode = "200", description = "Execucao concluida")
	@ApiResponse(responseCode = "400", description = "Integracao nao executavel")
	@PostMapping("/{id}/run")
	public ResponseEntity<IntegrationRunResponse> run(@PathVariable Long id) {
		return ResponseEntity.ok(new IntegrationRunResponse(runService.triggerManually(id)));
	}

	@Operation(summary = "Clonar integracao", description = "Cria uma copia da integracao (status rascunho) com o mesmo de->para.")
	@ApiResponse(responseCode = "201", description = "Integracao clonada")
	@PostMapping("/{id}/clone")
	public ResponseEntity<IntegrationResponse> clone(@PathVariable Long id) {
		Integration clone = service.clone(id);
		return ResponseEntity.status(HttpStatus.CREATED).body(new IntegrationResponse(clone));
	}

	@Operation(summary = "Remover integracao")
	@ApiResponse(responseCode = "204", description = "Integracao removida")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}
