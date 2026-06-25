package com.br.auction.garage.saved;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados para salvar um veiculo")
public class SavedVehicleRequest {

	@NotNull(message = "O veiculo e obrigatorio")
	@Schema(description = "ID do item de leilao (veiculo)")
	private Long auctionItemId;

	@Schema(description = "Anotacoes do usuario")
	private String notes;

	public Long getAuctionItemId() {
		return auctionItemId;
	}

	public void setAuctionItemId(Long auctionItemId) {
		this.auctionItemId = auctionItemId;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}
}
