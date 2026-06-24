package com.br.auction.response;

import java.util.List;
import java.util.stream.Collectors;

import com.br.auction.models.Auction;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta completa de um leilao com seus itens")
public class AuctionResponse extends AuctionListResponse {

	@Schema(description = "Lista de veiculos do leilao")
	private List<AuctionItemResponse> items;

	public AuctionResponse(Auction auction) {
		super(auction);
		if (auction.getItems() != null) {
			this.items = auction.getItems().stream().map(AuctionItemResponse::new).collect(Collectors.toList());
		}
	}

	public List<AuctionItemResponse> getItems() {
		return items;
	}
}
