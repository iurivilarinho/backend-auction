package com.br.auction.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lote de veículo do leilão")
public class LotResponse {

	@Schema(description = "Identificador interno do lote")
	private String lotId;

	@Schema(description = "Número do lote")
	private String lotNumber;

	@Schema(description = "Tipo do lote (CONSERVADO ou SUCATA)")
	private String lotType;

	@Schema(description = "Descrição do veículo")
	private String vehicleDescription;

	@Schema(description = "Valor atual do lance")
	private String currentBidValue;

	@Schema(description = "Nome do leilão")
	private String auctionName;

	@Schema(description = "Nome do pátio")
	private String yardName;

	public LotResponse() {
	}

	public String getLotId() {
		return lotId;
	}

	public void setLotId(String lotId) {
		this.lotId = lotId;
	}

	public String getLotNumber() {
		return lotNumber;
	}

	public void setLotNumber(String lotNumber) {
		this.lotNumber = lotNumber;
	}

	public String getLotType() {
		return lotType;
	}

	public void setLotType(String lotType) {
		this.lotType = lotType;
	}

	public String getVehicleDescription() {
		return vehicleDescription;
	}

	public void setVehicleDescription(String vehicleDescription) {
		this.vehicleDescription = vehicleDescription;
	}

	public String getCurrentBidValue() {
		return currentBidValue;
	}

	public void setCurrentBidValue(String currentBidValue) {
		this.currentBidValue = currentBidValue;
	}

	public String getAuctionName() {
		return auctionName;
	}

	public void setAuctionName(String auctionName) {
		this.auctionName = auctionName;
	}

	public String getYardName() {
		return yardName;
	}

	public void setYardName(String yardName) {
		this.yardName = yardName;
	}
}