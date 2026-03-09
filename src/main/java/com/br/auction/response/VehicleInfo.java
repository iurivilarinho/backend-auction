package com.br.auction.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados extraídos da descrição do veículo")
public class VehicleInfo {

	@Schema(description = "Marca")
	private String brand;

	@Schema(description = "Modelo")
	private String model;

	@Schema(description = "Ano")
	private String year;

	@Schema(description = "Tipo FIPE descoberto dinamicamente")
	private String vehicleType;

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

	public String getYear() {
		return year;
	}

	public void setYear(String year) {
		this.year = year;
	}

	public String getVehicleType() {
		return vehicleType;
	}

	public void setVehicleType(String vehicleType) {
		this.vehicleType = vehicleType;
	}
}