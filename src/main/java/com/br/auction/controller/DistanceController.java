package com.br.auction.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.br.auction.response.DistanceSettingResponse;
import com.br.auction.service.AuctionService;
import com.br.auction.service.DistanceService;
import com.br.auction.service.GeocodingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/distance")
@Tag(name = "Distancia", description = "Configuracao da origem e calculo de distancia ate as cidades dos leiloes")
public class DistanceController {

	private final DistanceService distanceService;
	private final GeocodingService geocodingService;
	private final AuctionService auctionService;

	public DistanceController(DistanceService distanceService, GeocodingService geocodingService,
			AuctionService auctionService) {
		this.distanceService = distanceService;
		this.geocodingService = geocodingService;
		this.auctionService = auctionService;
	}

	@Operation(summary = "Obter origem configurada", description = "Retorna a cidade/estado de origem usada no calculo de distancia.")
	@ApiResponse(responseCode = "200", description = "Configuracao retornada")
	@GetMapping("/settings")
	public ResponseEntity<DistanceSettingResponse> getSettings() {
		return ResponseEntity.ok(new DistanceSettingResponse(distanceService.getOrCreateSettings()));
	}

	@Operation(summary = "Atualizar origem", description = "Atualiza a cidade/estado de origem (parametrizavel) e geocodifica o novo ponto.")
	@ApiResponse(responseCode = "200", description = "Configuracao atualizada")
	@PutMapping("/settings")
	public ResponseEntity<DistanceSettingResponse> updateSettings(@Valid @RequestBody DistanceSettingRequest request) {
		return ResponseEntity
				.ok(new DistanceSettingResponse(distanceService.updateSettings(request.getOriginCity(),
						request.getOriginState())));
	}

	@Operation(summary = "Definir origem por ponto no mapa", description = "Define a origem do calculo de distancia por coordenadas (lat/lng) escolhidas no mapa. O ponto tem prioridade sobre a cidade.")
	@ApiResponse(responseCode = "200", description = "Ponto de origem atualizado")
	@PutMapping("/settings/point")
	public ResponseEntity<DistanceSettingResponse> updateOriginPoint(
			@Valid @RequestBody DistanceOriginPointRequest request) {
		return ResponseEntity.ok(new DistanceSettingResponse(distanceService.updateOriginPoint(
				request.getLatitude(), request.getLongitude(), request.getLabel(),
				request.getCity(), request.getState())));
	}

	@Operation(summary = "Aquecer geocodificacao das cidades dos leiloes", description = "Enfileira (ou resolve) as coordenadas das cidades dos leiloes do escopo informado para que as distancias apareçam nas listagens.")
	@ApiResponse(responseCode = "202", description = "Aquecimento iniciado")
	@PostMapping("/warmup")
	public ResponseEntity<java.util.Map<String, Object>> warmup(
			@RequestParam(required = false) Long auctionId,
			@RequestParam(required = false) String providerCode,
			@RequestParam(required = false) String stateCode) {
		distanceService.getOrCreateSettings();
		List<String[]> cities = auctionService.distinctAuctionCities(auctionId, providerCode, stateCode);
		for (String[] cityState : cities) {
			geocodingService.enqueue(cityState[0], cityState[1]);
		}
		java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
		body.put("queued", cities.size());
		body.put("message", "Geocodificacao das cidades enfileirada. As distancias aparecerao em instantes.");
		return ResponseEntity.accepted().body(body);
	}
}
