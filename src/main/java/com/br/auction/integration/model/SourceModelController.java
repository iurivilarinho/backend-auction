package com.br.auction.integration.model;

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
@RequestMapping("/api/integration/source-models")
@Tag(name = "Integracao - Modelos da fonte", description = "Cadastro dos modelos de dados entregues pelas fontes externas")
public class SourceModelController {

	private final SourceModelService service;

	public SourceModelController(SourceModelService service) {
		this.service = service;
	}

	@Operation(summary = "Listar modelos da fonte", description = "Lista paginada de modelos da fonte com busca por codigo/nome.")
	@ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
	@GetMapping
	public ResponseEntity<Page<SourceModelResponse>> findAll(
			@RequestParam(required = false) String search, Pageable pageable) {
		Page<SourceModelResponse> page = service.findAll(search, pageable).map(SourceModelResponse::new);
		return ResponseEntity.ok(page);
	}

	@Operation(summary = "Buscar modelo da fonte por ID")
	@ApiResponse(responseCode = "200", description = "Modelo da fonte encontrado")
	@ApiResponse(responseCode = "404", description = "Modelo da fonte nao encontrado")
	@GetMapping("/{id}")
	public ResponseEntity<SourceModelResponse> findById(@PathVariable Long id) {
		return ResponseEntity.ok(new SourceModelResponse(service.findById(id)));
	}

	@Operation(summary = "Criar modelo da fonte")
	@ApiResponse(responseCode = "201", description = "Modelo da fonte criado")
	@PostMapping
	public ResponseEntity<SourceModelResponse> create(@Valid @RequestBody SourceModelRequest request) {
		SourceModel created = service.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(new SourceModelResponse(created));
	}

	@Operation(summary = "Atualizar modelo da fonte")
	@ApiResponse(responseCode = "200", description = "Modelo da fonte atualizado")
	@ApiResponse(responseCode = "404", description = "Modelo da fonte nao encontrado")
	@PutMapping("/{id}")
	public ResponseEntity<SourceModelResponse> update(@PathVariable Long id,
			@Valid @RequestBody SourceModelRequest request) {
		return ResponseEntity.ok(new SourceModelResponse(service.update(id, request)));
	}

	@Operation(summary = "Remover modelo da fonte")
	@ApiResponse(responseCode = "204", description = "Modelo da fonte removido")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}
