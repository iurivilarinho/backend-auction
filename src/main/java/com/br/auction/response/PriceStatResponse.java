package com.br.auction.response;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estatistica de preco de arremate (lances encerrados) agrupada por marca/modelo/ano")
public class PriceStatResponse {

	@Schema(description = "Marca")
	private final String brand;

	@Schema(description = "Modelo (quando a granularidade inclui modelo)")
	private final String model;

	@Schema(description = "Ano (quando a granularidade inclui ano)")
	private final String vehicleYear;

	@Schema(description = "Quantidade de veiculos encerrados no grupo")
	private final long count;

	@Schema(description = "Valor medio de arremate")
	private final BigDecimal averageBid;

	@Schema(description = "Menor valor de arremate")
	private final BigDecimal minBid;

	@Schema(description = "Maior valor de arremate")
	private final BigDecimal maxBid;

	public PriceStatResponse(String brand, String model, String vehicleYear, long count, BigDecimal averageBid,
			BigDecimal minBid, BigDecimal maxBid) {
		this.brand = brand;
		this.model = model;
		this.vehicleYear = vehicleYear;
		this.count = count;
		this.averageBid = averageBid;
		this.minBid = minBid;
		this.maxBid = maxBid;
	}

	public String getBrand() {
		return brand;
	}

	public String getModel() {
		return model;
	}

	public String getVehicleYear() {
		return vehicleYear;
	}

	public long getCount() {
		return count;
	}

	public BigDecimal getAverageBid() {
		return averageBid;
	}

	public BigDecimal getMinBid() {
		return minBid;
	}

	public BigDecimal getMaxBid() {
		return maxBid;
	}
}
