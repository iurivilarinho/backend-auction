package com.br.auction.response;

import com.br.auction.models.DistanceSetting;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Configuracao atual da origem de calculo de distancia")
public class DistanceSettingResponse {

	@Schema(description = "Cidade de origem")
	private final String originCity;

	@Schema(description = "Estado (sigla) de origem")
	private final String originState;

	public DistanceSettingResponse(DistanceSetting setting) {
		this.originCity = setting.getOriginCity();
		this.originState = setting.getOriginState();
	}

	public String getOriginCity() {
		return originCity;
	}

	public String getOriginState() {
		return originState;
	}
}
