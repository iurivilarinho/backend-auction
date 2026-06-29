package com.br.auction.models;

import java.text.Normalizer;
import java.util.Locale;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Centroide (lat/long) de um município brasileiro (base do IBGE), usado para geocodificação
 * <b>offline</b> — sem depender de API externa nem de rate limit. É a fonte primária do
 * {@code GeocodingService}; o Nominatim fica só como fallback.
 */
@Entity
@Table(name = "tbCityCentroid", indexes = @Index(name = "IX_CityCentroid_lookup", columnList = "normalizedName,uf"))
@Schema(description = "Centroide (lat/long) de um município brasileiro (IBGE), para geocodificação offline")
public class CityCentroid {

	@Id
	@Schema(description = "Código IBGE do município")
	private Long ibgeCode;

	@Column(nullable = false)
	@Schema(description = "Nome oficial do município")
	private String name;

	@Column(nullable = false)
	@Schema(description = "Nome normalizado (maiúsculo, sem acento) para busca")
	private String normalizedName;

	@Column(nullable = false, length = 2)
	@Schema(description = "Sigla da UF")
	private String uf;

	@Column(nullable = false)
	@Schema(description = "Latitude do centroide")
	private double latitude;

	@Column(nullable = false)
	@Schema(description = "Longitude do centroide")
	private double longitude;

	/**
	 * Normaliza um nome de cidade para busca: maiúsculo, sem acentos, sem pontuação e com espaços
	 * colapsados. Usado tanto na carga (seed) quanto na consulta, para casar nomes equivalentes.
	 */
	public static String normalize(String value) {
		if (value == null) {
			return "";
		}
		String noAccents = Normalizer.normalize(value.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
		return noAccents.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9 ]", " ").replaceAll("\\s+", " ").trim();
	}

	public Long getIbgeCode() {
		return ibgeCode;
	}

	public void setIbgeCode(Long ibgeCode) {
		this.ibgeCode = ibgeCode;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNormalizedName() {
		return normalizedName;
	}

	public void setNormalizedName(String normalizedName) {
		this.normalizedName = normalizedName;
	}

	public String getUf() {
		return uf;
	}

	public void setUf(String uf) {
		this.uf = uf;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
}
