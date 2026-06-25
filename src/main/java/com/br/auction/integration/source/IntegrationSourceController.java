package com.br.auction.integration.source;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/integration/sources")
@Tag(name = "Integracao - Fontes", description = "Cadastro de fontes externas (provedores) de onde os dados sao coletados")
public class IntegrationSourceController {

	private final IntegrationSourceService service;

	public IntegrationSourceController(IntegrationSourceService service) {
		this.service = service;
	}

	@Operation(summary = "Listar fontes", description = "Lista paginada de fontes com busca por codigo/nome.")
	@ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
	@GetMapping
	public ResponseEntity<Page<IntegrationSourceResponse>> findAll(
			@RequestParam(required = false) String search, Pageable pageable) {
		Page<IntegrationSourceResponse> page = service.findAll(search, pageable).map(IntegrationSourceResponse::new);
		return ResponseEntity.ok(page);
	}

	@Operation(summary = "Buscar fonte por ID")
	@ApiResponse(responseCode = "200", description = "Fonte encontrada")
	@ApiResponse(responseCode = "404", description = "Fonte nao encontrada")
	@GetMapping("/{id}")
	public ResponseEntity<IntegrationSourceResponse> findById(@PathVariable Long id) {
		return ResponseEntity.ok(new IntegrationSourceResponse(service.findById(id)));
	}

	@Operation(summary = "Criar fonte")
	@ApiResponse(responseCode = "201", description = "Fonte criada")
	@PostMapping
	public ResponseEntity<IntegrationSourceResponse> create(@Valid @RequestBody IntegrationSourceRequest request) {
		IntegrationSource created = service.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(new IntegrationSourceResponse(created));
	}

	@Operation(summary = "Atualizar fonte")
	@ApiResponse(responseCode = "200", description = "Fonte atualizada")
	@ApiResponse(responseCode = "404", description = "Fonte nao encontrada")
	@PutMapping("/{id}")
	public ResponseEntity<IntegrationSourceResponse> update(@PathVariable Long id,
			@Valid @RequestBody IntegrationSourceRequest request) {
		return ResponseEntity.ok(new IntegrationSourceResponse(service.update(id, request)));
	}

	@Operation(summary = "Remover fonte")
	@ApiResponse(responseCode = "204", description = "Fonte removida")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}
