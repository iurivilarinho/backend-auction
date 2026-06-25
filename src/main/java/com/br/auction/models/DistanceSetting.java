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
}
