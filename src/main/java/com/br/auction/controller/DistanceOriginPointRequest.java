package com.br.auction.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Define o ponto de origem do calculo de distancia por coordenadas (escolhido no mapa). */
@Schema(description = "Ponto de origem (lat/lng) escolhido no mapa")
public class DistanceOriginPointRequest {

	@NotNull
	@Schema(description = "Latitude do ponto", example = "-18.9686")
	private Double latitude;

	@NotNull
	@Schema(description = "Longitude do ponto", example = "-49.4648")
	private Double longitude;

	@Schema(description = "Rotulo opcional do ponto", example = "Minha loja")
	private String label;

	@Schema(description = "Cidade aproximada do ponto (opcional, para exibicao)")
	private String city;

	@Schema(description = "UF aproximada do ponto (opcional)")
	private String state;

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}
}
