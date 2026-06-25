package com.br.auction.response;

import com.br.auction.models.DistanceSetting;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Configuracao atual da origem de calculo de distancia")
public class DistanceSettingResponse {

	@Schema(description = "Cidade de origem")
	private final String originCity;

	@Schema(description = "Estado (sigla) de origem")
	private final String originState;

	@Schema(description = "Latitude do ponto de origem (quando definido no mapa)")
	private final Double originLatitude;

	@Schema(description = "Longitude do ponto de origem (quando definido no mapa)")
	private final Double originLongitude;

	@Schema(description = "Rotulo do ponto escolhido no mapa")
	private final String originLabel;

	@Schema(description = "Indica se a origem usa um ponto exato (mapa) em vez da cidade")
	private final boolean usingPoint;

	public DistanceSettingResponse(DistanceSetting setting) {
		this.originCity = setting.getOriginCity();
		this.originState = setting.getOriginState();
		this.originLatitude = setting.getOriginLatitude();
		this.originLongitude = setting.getOriginLongitude();
		this.originLabel = setting.getOriginLabel();
		this.usingPoint = setting.hasCoordinates();
	}

	public String getOriginCity() {
		return originCity;
	}

	public String getOriginState() {
		return originState;
	}

	public Double getOriginLatitude() {
		return originLatitude;
	}

	public Double getOriginLongitude() {
		return originLongitude;
	}

	public String getOriginLabel() {
		return originLabel;
	}

	public boolean isUsingPoint() {
		return usingPoint;
	}
}
