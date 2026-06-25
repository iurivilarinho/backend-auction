package com.br.auction.models;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "tbCityGeocode", uniqueConstraints = @UniqueConstraint(columnNames = { "city", "state" }))
@Schema(description = "Coordenadas geocodificadas de uma cidade, usadas para calcular distancias (cache)")
public class CityGeocode {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Schema(description = "Identificador interno")
	private Long id;

	@Column(nullable = false)
	@Schema(description = "Nome da cidade (normalizado)")
	private String city;

	@Column(nullable = false)
	@Schema(description = "Sigla do estado")
	private String state;

	@Column
	@Schema(description = "Latitude")
	private Double latitude;

	@Column
	@Schema(description = "Longitude")
	private Double longitude;

	@Column(nullable = false)
	@Schema(description = "Indica se a geocodificacao encontrou coordenadas")
	private boolean resolved;

	@Column(nullable = false)
	@Schema(description = "Momento da geocodificacao")
	private LocalDateTime updatedAt = LocalDateTime.now();

	public Long getId() {
		return id;
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

	public boolean isResolved() {
		return resolved;
	}

	public void setResolved(boolean resolved) {
		this.resolved = resolved;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
