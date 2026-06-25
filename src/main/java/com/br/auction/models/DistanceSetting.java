package com.br.auction.models;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Ponto de partida configuravel para o calculo de distancias. Mantido como linha unica
 * (id fixo) para que o usuario parametrize a origem usada em todos os calculos.
 */
@Entity
@Table(name = "tbDistanceSetting")
@Schema(description = "Configuracao do ponto de origem para calculo de distancia")
public class DistanceSetting {

	public static final Long SINGLETON_ID = 1L;

	@Id
	@Schema(description = "Identificador fixo (linha unica)")
	private Long id = SINGLETON_ID;

	@Column(nullable = false)
	@Schema(description = "Cidade de origem do calculo de distancia")
	private String originCity;

	@Column(nullable = false)
	@Schema(description = "Estado (sigla) da cidade de origem")
	private String originState;

	@Column
	@Schema(description = "Latitude do ponto de origem (definido no mapa); tem prioridade sobre a cidade")
	private Double originLatitude;

	@Column
	@Schema(description = "Longitude do ponto de origem (definido no mapa); tem prioridade sobre a cidade")
	private Double originLongitude;

	@Column(length = 200)
	@Schema(description = "Rotulo/descricao do ponto de origem escolhido no mapa")
	private String originLabel;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

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

	public Double getOriginLatitude() {
		return originLatitude;
	}

	public void setOriginLatitude(Double originLatitude) {
		this.originLatitude = originLatitude;
	}

	public Double getOriginLongitude() {
		return originLongitude;
	}

	public void setOriginLongitude(Double originLongitude) {
		this.originLongitude = originLongitude;
	}

	public String getOriginLabel() {
		return originLabel;
	}

	public void setOriginLabel(String originLabel) {
		this.originLabel = originLabel;
	}

	/** Verdadeiro quando ha um ponto (lat/lng) explicito definido no mapa. */
	public boolean hasCoordinates() {
		return originLatitude != null && originLongitude != null;
	}
}
