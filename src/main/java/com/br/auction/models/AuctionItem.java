package com.br.auction.models;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbAuctionItem")
@Schema(description = "Entidade que representa um veiculo dentro de um leilao")
public class AuctionItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Schema(description = "Identificador interno do item")
	private Long id;

	@Column
	@Schema(description = "Identificador do lote no portal do provedor")
	private String lotId;

	@Column
	@Schema(description = "Numero do lote")
	private String lotNumber;

	@Column
	@Schema(description = "Tipo do lote")
	private String lotType;

	@Column
	@Schema(description = "Descricao do veiculo")
	private String vehicleDescription;

	@Column(precision = 15, scale = 2)
	@Schema(description = "Valor atual do lance")
	private BigDecimal currentBidValue;

	@Column(precision = 15, scale = 2)
	@Schema(description = "Valor da tabela FIPE")
	private BigDecimal fipeValue;

	@OneToMany(mappedBy = "auctionItem", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("position ASC")
	@Schema(description = "Imagens do veiculo armazenadas no banco")
	private List<AuctionItemImage> images = new ArrayList<>();

	@ManyToOne
	@JoinColumn(name = "fk_Id_Auction", foreignKey = @ForeignKey(name = "FK_FROM_TBAUCTIONITEM_FOR_TBAUCTION"))
	@Schema(description = "Leilao ao qual o veiculo pertence")
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

	public List<AuctionItemImage> getImages() {
		return images;
	}

	public void setImages(List<AuctionItemImage> images) {
		this.images = images == null ? new ArrayList<>() : images;
	}

	public void clearImages() {
		this.images.clear();
	}

	public void addImage(AuctionItemImage image) {
		image.setAuctionItem(this);
		image.setPosition(this.images.size());
		this.images.add(image);
	}

	public Auction getAuction() {
		return auction;
	}

	public void setAuction(Auction auction) {
		this.auction = auction;
	}
}
