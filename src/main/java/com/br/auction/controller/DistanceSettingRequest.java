package com.br.auction.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados para atualizar a origem do calculo de distancia")
public class DistanceSettingRequest {

	@NotBlank
	@Schema(description = "Cidade de origem", example = "Ituiutaba")
	private String originCity;

	@NotBlank
	@Schema(description = "Estado (sigla) de origem", example = "MG")
	private String originState;

	public String getOriginCity() {
		return originCity;
	}

	public void setOriginCity(String originCity) {
		this.originCity = originCity;
	}

	public String getOriginState() {
		return originState;
	}

	public void setOriginState(String originState) {
		this.originState = originState;
	}
}
