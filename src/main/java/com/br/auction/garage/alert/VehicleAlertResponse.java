package com.br.auction.garage.alert;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.br.auction.garage.enums.AlertType;
import com.br.auction.garage.models.VehicleAlert;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Alerta de veiculo")
public class VehicleAlertResponse {

	private final Long id;
	private final String name;
	private final AlertType type;
	private final String typeLabel;
	private final String keyword;
	private final String brand;
	private final String model;
	private final String city;
	private final String lotType;
	private final BigDecimal maxBid;
	private final Double radiusKm;
	private final Integer minYear;
	private final BigDecimal thresholdValue;
	private final BigDecimal soldBelowValue;
	private final Integer fipePercent;
	private final Integer leadTimeMinutes;
	private final Boolean notifyNewMatch;
	private final Boolean notifyOnStart;
	private final Boolean notifyPriceAbove;
	private final Boolean notifyFipeDeal;
	private final Boolean notifyClosingSoon;
	private final Boolean notifySoldBelow;
	private final String recipientPhone;
	private final Boolean active;
	private final int matches;
	private final LocalDateTime createdAt;

	public VehicleAlertResponse(VehicleAlert alert, int matches) {
		this.id = alert.getId();
		this.name = alert.getName();
		this.type = alert.getType();
		this.typeLabel = alert.getType() == null ? null : alert.getType().getDescription();
		this.keyword = alert.getKeyword();
		this.brand = alert.getBrand();
		this.model = alert.getModel();
		this.city = alert.getCity();
		this.lotType = alert.getLotType();
		this.maxBid = alert.getMaxBid();
		this.radiusKm = alert.getRadiusKm();
		this.minYear = alert.getMinYear();
		this.thresholdValue = alert.getThresholdValue();
		this.soldBelowValue = alert.getSoldBelowValue();
		this.fipePercent = alert.getFipePercent();
		this.leadTimeMinutes = alert.getLeadTimeMinutes();
		this.notifyNewMatch = Boolean.TRUE.equals(alert.getNotifyNewMatch());
		this.notifyOnStart = Boolean.TRUE.equals(alert.getNotifyOnStart());
		this.notifyPriceAbove = Boolean.TRUE.equals(alert.getNotifyPriceAbove());
		this.notifyFipeDeal = Boolean.TRUE.equals(alert.getNotifyFipeDeal());
		this.notifyClosingSoon = Boolean.TRUE.equals(alert.getNotifyClosingSoon());
		this.notifySoldBelow = Boolean.TRUE.equals(alert.getNotifySoldBelow());
		this.recipientPhone = alert.getRecipientPhone();
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

	public AlertType getType() {
		return type;
	}

	public String getTypeLabel() {
		return typeLabel;
	}

	public String getKeyword() {
		return keyword;
	}

	public String getBrand() {
		return brand;
	}

	public String getModel() {
		return model;
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

	public Double getRadiusKm() {
		return radiusKm;
	}

	public Integer getMinYear() {
		return minYear;
	}

	public BigDecimal getThresholdValue() {
		return thresholdValue;
	}

	public BigDecimal getSoldBelowValue() {
		return soldBelowValue;
	}

	public Integer getFipePercent() {
		return fipePercent;
	}

	public Integer getLeadTimeMinutes() {
		return leadTimeMinutes;
	}

	public Boolean getNotifyNewMatch() {
		return notifyNewMatch;
	}

	public Boolean getNotifyPriceAbove() {
		return notifyPriceAbove;
	}

	public Boolean getNotifyFipeDeal() {
		return notifyFipeDeal;
	}

	public Boolean getNotifyClosingSoon() {
		return notifyClosingSoon;
	}

	public Boolean getNotifySoldBelow() {
		return notifySoldBelow;
	}

	public Boolean getNotifyOnStart() {
		return notifyOnStart;
	}

	public String getRecipientPhone() {
		return recipientPhone;
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
