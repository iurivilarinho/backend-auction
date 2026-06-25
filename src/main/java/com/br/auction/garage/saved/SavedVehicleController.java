package com.br.auction.garage.saved;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/garage/saved")
@Tag(name = "Garagem - Salvos", description = "Veiculos salvos (favoritos)")
public class SavedVehicleController {

	private final SavedVehicleService service;

	public SavedVehicleController(SavedVehicleService service) {
		this.service = service;
	}

	@Operation(summary = "Listar veiculos salvos")
	@ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
	@GetMapping
	public ResponseEntity<List<SavedVehicleResponse>> findAll() {
		return ResponseEntity.ok(service.findAll().stream().map(SavedVehicleResponse::new).toList());
	}

	@Operation(summary = "Salvar um veiculo")
	@ApiResponse(responseCode = "201", description = "Veiculo salvo")
	@PostMapping
	public ResponseEntity<SavedVehicleResponse> save(@Valid @RequestBody SavedVehicleRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(new SavedVehicleResponse(service.save(request)));
	}

	@Operation(summary = "Remover um veiculo salvo")
	@ApiResponse(responseCode = "204", description = "Removido")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}
