package com.br.auction.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Leilão no formato do feed do provedor (mesmo contrato JSON consumido pelo módulo de integração).
 * Substitui o antigo {@code Map<String, Object>} por um modelo tipado e documentado.
 */
@Schema(description = "Leilão do feed do provedor")
public class AuctionFeedResponse {

	@Schema(description = "Identificador do leilão na fonte")
	private final String auctionId;

	@Schema(description = "Número/edital do leilão")
	private final String auctionNoticeNumber;

	@Schema(description = "Cidade do leilão")
	private final String city;

	@Schema(description = "Leiloeiro responsável")
	private final String auctioneer;

	@Schema(description = "Situação do leilão na fonte")
	private final String status;

	@Schema(description = "Data/hora de encerramento (formato da fonte)")
	private final String closingDate;

	@Schema(description = "Ano do leilão")
	private final String auctionYear;

	@Schema(description = "URL de origem (edital ou site)")
	private final String sourceUrl;

	public AuctionFeedResponse(String auctionId, String auctionNoticeNumber, String city, String auctioneer,
			String status, String closingDate, String auctionYear, String sourceUrl) {
		this.auctionId = auctionId;
		this.auctionNoticeNumber = auctionNoticeNumber;
		this.city = city;
		this.auctioneer = auctioneer;
		this.status = status;
		this.closingDate = closingDate;
		this.auctionYear = auctionYear;
		this.sourceUrl = sourceUrl;
	}

	public AuctionFeedResponse(AuctionListJsonResponse auction) {
		this(auction.getAuctionId(), auction.getAuctionNoticeNumber(), auction.getCity(), auction.getAuctioneer(),
				auction.getStatus(), auction.getClosingDate(), auction.getAuctionYear(), auction.getSourceUrl());
	}

	public String getAuctionId() {
		return auctionId;
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

	public String getClosingDate() {
		return closingDate;
	}

	public String getAuctionYear() {
		return auctionYear;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}
}
