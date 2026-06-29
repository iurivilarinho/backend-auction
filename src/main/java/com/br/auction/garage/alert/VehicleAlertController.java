package com.br.auction.garage.alert;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.auction.garage.models.VehicleAlert;
import com.br.auction.response.EnumOptionResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/garage/alerts")
@Tag(name = "Garagem - Alertas", description = "Alertas de veiculos por criterio")
public class VehicleAlertController {

	private final VehicleAlertService service;
	private final AlertScheduler scheduler;
	private final AlertFindingService findingService;

	public VehicleAlertController(VehicleAlertService service, AlertScheduler scheduler,
			AlertFindingService findingService) {
		this.service = service;
		this.scheduler = scheduler;
		this.findingService = findingService;
	}

	@Operation(summary = "Listar alertas")
	@GetMapping
	public ResponseEntity<List<VehicleAlertResponse>> findAll() {
		List<VehicleAlertResponse> list = service.findAll().stream()
				.map(alert -> new VehicleAlertResponse(alert, service.countMatches(alert)))
				.toList();
		return ResponseEntity.ok(list);
	}

	@Operation(summary = "Listar tipos de alerta disponiveis")
	@GetMapping("/types")
	public ResponseEntity<List<EnumOptionResponse>> types() {
		return ResponseEntity.ok(service.types());
	}

	@Operation(summary = "Avaliar os alertas agora (disparo manual)")
	@PostMapping("/run")
	public ResponseEntity<Void> run() {
		scheduler.triggerNow();
		return ResponseEntity.accepted().build();
	}

	@Operation(summary = "Achados: veiculos encontrados pelos alertas (todos)")
	@GetMapping("/findings")
	public ResponseEntity<List<AlertFindingResponse>> findings() {
		return ResponseEntity.ok(findingService.findAll());
	}

	@Operation(summary = "Achados de um alerta especifico")
	@GetMapping("/{id}/findings")
	public ResponseEntity<List<AlertFindingResponse>> findingsByAlert(@PathVariable Long id) {
		return ResponseEntity.ok(findingService.findByAlert(id));
	}

	@Operation(summary = "Criar alerta")
	@ApiResponse(responseCode = "201", description = "Alerta criado")
	@PostMapping
	public ResponseEntity<VehicleAlertResponse> create(@Valid @RequestBody VehicleAlertRequest request) {
		VehicleAlert alert = service.create(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new VehicleAlertResponse(alert, service.countMatches(alert)));
	}

	@Operation(summary = "Atualizar alerta")
	@PutMapping("/{id}")
	public ResponseEntity<VehicleAlertResponse> update(@PathVariable Long id,
			@Valid @RequestBody VehicleAlertRequest request) {
		VehicleAlert alert = service.update(id, request);
		return ResponseEntity.ok(new VehicleAlertResponse(alert, service.countMatches(alert)));
	}

	@Operation(summary = "Remover alerta")
	@ApiResponse(responseCode = "204", description = "Removido")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}
