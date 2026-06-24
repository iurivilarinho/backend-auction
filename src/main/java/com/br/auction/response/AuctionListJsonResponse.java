package com.br.auction.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Leilao disponivel no portal do provedor")
public class AuctionListJsonResponse {

	@Schema(description = "Numero do edital do leilao")
	private String auctionNoticeNumber;

	@Schema(description = "Cidade do leilao")
	private String city;

	@Schema(description = "Nome do patio ou leiloeiro")
	private String auctioneer;

	@Schema(description = "Status do leilao")
	private String status;

	@Schema(description = "Data de encerramento")
	private String closingDate;

	@Schema(description = "Id interno usado para buscar os lotes")
	private String auctionId;

	@Schema(description = "Ano do leilao")
	private String auctionYear;

	@Schema(description = "URL publica do leilao no provedor")
	private String sourceUrl;

	public AuctionListJsonResponse() {
	}

	public String getAuctionNoticeNumber() {
		return auctionNoticeNumber;
	}

	public void setAuctionNoticeNumber(String auctionNoticeNumber) {
		this.auctionNoticeNumber = auctionNoticeNumber;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getAuctioneer() {
		return auctioneer;
	}

	public void setAuctioneer(String auctioneer) {
		this.auctioneer = auctioneer;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getClosingDate() {
		return closingDate;
	}

	public void setClosingDate(String closingDate) {
		this.closingDate = closingDate;
	}

	public String getAuctionId() {
		return auctionId;
	}

	public void setAuctionId(String auctionId) {
		this.auctionId = auctionId;
	}

	public String getAuctionYear() {
		return auctionYear;
	}

	public void setAuctionYear(String auctionYear) {
		this.auctionYear = auctionYear;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public void setSourceUrl(String sourceUrl) {
		this.sourceUrl = sourceUrl;
	}
}
