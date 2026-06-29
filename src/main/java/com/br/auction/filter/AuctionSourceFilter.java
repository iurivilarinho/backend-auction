package com.br.auction.filter;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Filtros aplicados na coleta de leilões direto na fonte (provedor)")
public class AuctionSourceFilter {

	private String auctionNumber;
	private String closingDateStart;
	private String closingDateEnd;
	private String municipalityId;
	private String vehicleTypeCode;
	private String brand;
	private String model;
	private String vehicleYear;
	private String color;
	private String condition;

	public boolean hasAnySourceFilter() {
		return hasText(auctionNumber) || hasText(closingDateStart) || hasText(closingDateEnd) || hasText(municipalityId)
				|| hasText(vehicleTypeCode) || hasText(brand) || hasText(model) || hasText(vehicleYear)
				|| hasText(color) || hasText(condition);
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	public String getAuctionNumber() {
		return auctionNumber;
	}

	public void setAuctionNumber(String auctionNumber) {
		this.auctionNumber = auctionNumber;
	}

	public String getClosingDateStart() {
		return closingDateStart;
	}

	public void setClosingDateStart(String closingDateStart) {
		this.closingDateStart = closingDateStart;
	}

	public String getClosingDateEnd() {
		return closingDateEnd;
	}

	public void setClosingDateEnd(String closingDateEnd) {
		this.closingDateEnd = closingDateEnd;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public String getVehicleTypeCode() {
		return vehicleTypeCode;
	}

	public void setVehicleTypeCode(String vehicleTypeCode) {
		this.vehicleTypeCode = vehicleTypeCode;
	}

	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getVehicleYear() {
		return vehicleYear;
	}

	public void setVehicleYear(String vehicleYear) {
		this.vehicleYear = vehicleYear;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}
}
