package com.br.auction.response;

import java.time.LocalDateTime;

import com.br.auction.models.Auction;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta simplificada de leilão para listagens")
public class AuctionListResponse {

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

	@Schema(description = "Ano do leilão")
	private String auctionYear;

	public AuctionListResponse(Auction auction) {
		this.id = auction.getId();
		this.auctionNoticeNumber = auction.getAuctionNoticeNumber();
		this.city = auction.getCity();
		this.auctioneer = auction.getAuctioneer();
		this.status = auction.getStatus();
		this.closingDate = auction.getClosingDate();
		this.auctionYear = auction.getAuctionYear();
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

	public String getAuctionYear() {
		return auctionYear;
	}
}