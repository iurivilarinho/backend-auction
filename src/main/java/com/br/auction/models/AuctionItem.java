package com.br.auction.models;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

@Entity
@Table(name = "tbAuctionItem")
@Schema(description = "Entidade que representa um veículo dentro de um leilão")
public class AuctionItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Schema(description = "Identificador interno do item")
	private Long id;

	@Column
	@Schema(description = "Identificador do lote no portal do DETRAN")
	private String lotId;

	@Column
	@Schema(description = "Número do lote")
	private String lotNumber;

	@Column
	@Schema(description = "Tipo do lote (CONSERVADO ou SUCATA)")
	private String lotType;

	@Column
	@Schema(description = "Descrição do veículo")
	private String vehicleDescription;

	@Column(precision = 15, scale = 2)
	@Schema(description = "Valor atual do lance")
	private BigDecimal currentBidValue;

	@Column(precision = 15, scale = 2)
	@Schema(description = "Valor da tabela FIPE")
	private BigDecimal fipeValue;

	@ManyToOne
	@JoinColumn(name = "fk_Id_Auction", foreignKey = @ForeignKey(name = "FK_FROM_TBAUCTIONITEM_FOR_TBAUCTION"))
	@Schema(description = "Leilão ao qual o veículo pertence")
	private Auction auction;

	public Long getId() {
		return id;
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

	public BigDecimal getCurrentBidValue() {
		return currentBidValue;
	}

	public void setCurrentBidValue(BigDecimal currentBidValue) {
		this.currentBidValue = currentBidValue;
	}

	public BigDecimal getFipeValue() {
		return fipeValue;
	}

	public void setFipeValue(BigDecimal fipeValue) {
		this.fipeValue = fipeValue;
	}

	public Auction getAuction() {
		return auction;
	}

	public void setAuction(Auction auction) {
		this.auction = auction;
	}
}