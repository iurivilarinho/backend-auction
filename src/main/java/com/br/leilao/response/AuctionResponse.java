package com.br.leilao.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.br.leilao.models.Auction;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta completa de um leilão com seus itens")
public class AuctionResponse {

	@Schema(description = "Identificador do leilão")
	private Long id;

	@Schema(description = "Número do edital do leilão")
	private String auctionNoticeNumber;

	@Schema(description = "Cidade onde ocorre o leilão")
	private String city;

	@Schema(description = "Nome do leiloeiro ou pátio")
	private String auctioneer;

	@Schema(description = "Status do leilão")
	private String status;

	@Schema(description = "Data de encerramento do leilão")
	private LocalDateTime closingDate;

	@Schema(description = "Identificador do leilão no portal do DETRAN")
	private String detranAuctionId;

	@Schema(description = "Ano do leilão")
	private String auctionYear;

	@Schema(description = "Lista de veículos do leilão")
	private List<AuctionItemResponse> items;

	public AuctionResponse(Auction auction) {
		this.id = auction.getId();
		this.auctionNoticeNumber = auction.getAuctionNoticeNumber();
		this.city = auction.getCity();
		this.auctioneer = auction.getAuctioneer();
		this.status = auction.getStatus();
		this.closingDate = auction.getClosingDate();
		this.detranAuctionId = auction.getDetranAuctionId();
		this.auctionYear = auction.getAuctionYear();

		if (auction.getItems() != null) {
			this.items = auction.getItems().stream().map(AuctionItemResponse::new).collect(Collectors.toList());
		}
	}

	public Long getId() {
		return id;
	}

	public String getAuctionNoticeNumber() {
		return auctionNoticeNumber;
	}

	public String getCity() {
		return city;
	}

	public String getAuctioneer() {
		return auctioneer;
	}

	public String getStatus() {
		return status;
	}

	public LocalDateTime getClosingDate() {
		return closingDate;
	}

	public String getDetranAuctionId() {
		return detranAuctionId;
	}

	public String getAuctionYear() {
		return auctionYear;
	}

	public List<AuctionItemResponse> getItems() {
		return items;
	}
}