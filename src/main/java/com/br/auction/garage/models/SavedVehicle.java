package com.br.auction.garage.models;

import java.time.LocalDateTime;

import com.br.auction.models.AuctionItem;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbSavedVehicle")
@Schema(description = "Veiculo salvo (favorito) pelo usuario")
public class SavedVehicle {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "fk_Id_AuctionItem", foreignKey = @ForeignKey(name = "FK_FROM_TBSAVEDVEHICLE_FOR_TBAUCTIONITEM"))
	@Schema(description = "Veiculo (lote) salvo")
	private AuctionItem auctionItem;

	@Column(length = 1000)
	@Schema(description = "Anotacoes do usuario sobre o veiculo")
	private String notes;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	public void onCreate() {
		this.createdAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public AuctionItem getAuctionItem() {
		return auctionItem;
	}

	public void setAuctionItem(AuctionItem auctionItem) {
		this.auctionItem = auctionItem;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
