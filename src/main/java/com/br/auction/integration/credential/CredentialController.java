package com.br.auction.integration.credential;

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
@RequestMapping("/api/integration/credentials")
@Tag(name = "Integracao - Credenciais", description = "Cadastro de credenciais usadas para autenticar nas fontes externas")
public class CredentialController {

	private final CredentialService service;

	public CredentialController(CredentialService service) {
		this.service = service;
	}

	@Operation(summary = "Listar credenciais", description = "Lista paginada de credenciais com busca por codigo/nome.")
	@ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
	@GetMapping
	public ResponseEntity<Page<CredentialResponse>> findAll(
			@RequestParam(required = false) String search, Pageable pageable) {
		Page<CredentialResponse> page = service.findAll(search, pageable).map(CredentialResponse::new);
		return ResponseEntity.ok(page);
	}

	@Operation(summary = "Buscar credencial por ID")
	@ApiResponse(responseCode = "200", description = "Credencial encontrada")
	@ApiResponse(responseCode = "404", description = "Credencial nao encontrada")
	@GetMapping("/{id}")
	public ResponseEntity<CredentialResponse> findById(@PathVariable Long id) {
		return ResponseEntity.ok(new CredentialResponse(service.findById(id)));
	}

	@Operation(summary = "Criar credencial")
	@ApiResponse(responseCode = "201", description = "Credencial criada")
	@PostMapping
	public ResponseEntity<CredentialResponse> create(@Valid @RequestBody CredentialRequest request) {
		Credential created = service.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(new CredentialResponse(created));
	}

	@Operation(summary = "Atualizar credencial")
	@ApiResponse(responseCode = "200", description = "Credencial atualizada")
	@ApiResponse(responseCode = "404", description = "Credencial nao encontrada")
	@PutMapping("/{id}")
	public ResponseEntity<CredentialResponse> update(@PathVariable Long id,
			@Valid @RequestBody CredentialRequest request) {
		return ResponseEntity.ok(new CredentialResponse(service.update(id, request)));
	}

	@Operation(summary = "Remover credencial")
	@ApiResponse(responseCode = "204", description = "Credencial removida")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}
