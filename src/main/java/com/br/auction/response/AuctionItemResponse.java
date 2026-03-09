package com.br.auction.response;

import java.math.BigDecimal;

import com.br.auction.models.AuctionItem;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta de item de leilão")
public class AuctionItemResponse {

	@Schema(description = "Identificador do item")
	private Long id;

	@Schema(description = "Identificador do lote no portal do DETRAN")
	private String lotId;

	@Schema(description = "Número do lote")
	private String lotNumber;

	@Schema(description = "Tipo do lote (CONSERVADO ou SUCATA)")
	private String lotType;

	@Schema(description = "Descrição do veículo")
	private String vehicleDescription;

	@Schema(description = "Valor atual do lance")
	private BigDecimal currentBidValue;

	@Schema(description = "Valor da tabela FIPE")
	private BigDecimal fipeValue;

	@Schema(description = "Leilão ao qual o veículo pertence")
	private AuctionListResponse auction;

	public AuctionItemResponse(AuctionItem item) {
		this.id = item.getId();
		this.lotId = item.getLotId();
		this.lotNumber = item.getLotNumber();
		this.lotType = item.getLotType();
		this.vehicleDescription = item.getVehicleDescription();
		this.currentBidValue = item.getCurrentBidValue();
		this.fipeValue = item.getFipeValue();
		this.auction = item.getAuction() != null ? new AuctionListResponse(item.getAuction()) : null;
	}

	public Long getId() {
		return id;
	}

	public String getLotId() {
		return lotId;
	}

	public String getLotNumber() {
		return lotNumber;
	}

	public String getLotType() {
		return lotType;
	}

	public String getVehicleDescription() {
		return vehicleDescription;
	}

	public BigDecimal getCurrentBidValue() {
		return currentBidValue;
	}

	public BigDecimal getFipeValue() {
		return fipeValue;
	}

	public AuctionListResponse getAuction() {
		return auction;
	}

}