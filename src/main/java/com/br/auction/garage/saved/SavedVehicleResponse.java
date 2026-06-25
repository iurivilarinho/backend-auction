package com.br.auction.garage.saved;

import java.time.LocalDateTime;

import com.br.auction.garage.models.SavedVehicle;
import com.br.auction.response.AuctionItemResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Veiculo salvo")
public class SavedVehicleResponse {

	private final Long id;
	private final AuctionItemResponse vehicle;
	private final String notes;
	private final LocalDateTime createdAt;

	public SavedVehicleResponse(SavedVehicle saved) {
		this.id = saved.getId();
		this.vehicle = saved.getAuctionItem() == null ? null : new AuctionItemResponse(saved.getAuctionItem());
		this.notes = saved.getNotes();
		this.createdAt = saved.getCreatedAt();
	}

	public Long getId() {
		return id;
	}

	public AuctionItemResponse getVehicle() {
		return vehicle;
	}

	public String getNotes() {
		return notes;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
