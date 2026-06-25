package com.br.auction.garage.alert;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.br.auction.garage.models.VehicleAlert;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Alerta de veiculo")
public class VehicleAlertResponse {

	private final Long id;
	private final String name;
	private final String keyword;
	private final String city;
	private final String lotType;
	private final BigDecimal maxBid;
	private final Boolean active;
	private final int matches;
	private final LocalDateTime createdAt;

	public VehicleAlertResponse(VehicleAlert alert, int matches) {
		this.id = alert.getId();
		this.name = alert.getName();
		this.keyword = alert.getKeyword();
		this.city = alert.getCity();
		this.lotType = alert.getLotType();
		this.maxBid = alert.getMaxBid();
		this.active = alert.getActive();
		this.matches = matches;
		this.createdAt = alert.getCreatedAt();
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getKeyword() {
		return keyword;
	}

	public String getCity() {
		return city;
	}

	public String getLotType() {
		return lotType;
	}

	public BigDecimal getMaxBid() {
		return maxBid;
	}

	public Boolean getActive() {
		return active;
	}

	public int getMatches() {
		return matches;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
