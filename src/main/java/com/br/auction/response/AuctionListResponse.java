package com.br.auction.response;

import java.time.LocalDateTime;

import com.br.auction.enums.AuctionStatus;
import com.br.auction.models.Auction;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta simplificada de leilao para listagens")
public class AuctionListResponse {

	@Schema(description = "Identificador do leilao")
	private Long id;

	@Schema(description = "Numero do edital do leilao")
	private String auctionNoticeNumber;

	@Schema(description = "Cidade onde ocorre o leilao")
	private String city;

	@Schema(description = "Nome do leiloeiro ou patio")
	private String auctioneer;

	@Schema(description = "Status textual retornado pelo provedor")
	private String status;

	@Schema(description = "Status normalizado para filtros da API")
	private AuctionStatus normalizedStatus;

	@Schema(description = "Rotulo do status")
	private String statusLabel;

	@Schema(description = "Data de encerramento do leilao")
	private LocalDateTime closingDate;

	@Schema(description = "Identificador do leilao no provedor")
	private String detranAuctionId;

	@Schema(description = "Ano do leilao")
	private String auctionYear;

	@Schema(description = "Codigo do provedor")
	private String providerCode;

	@Schema(description = "Nome do provedor")
	private String providerName;

	@Schema(description = "Codigo do estado")
	private String stateCode;

	@Schema(description = "Nome do estado")
	private String stateName;

	@Schema(description = "URL publica do leilao no provedor")
	private String sourceUrl;

	@Schema(description = "Quantidade de itens carregados para o leilao")
	private Integer itemCount;

	public AuctionListResponse(Auction auction) {
		this.id = auction.getId();
		this.auctionNoticeNumber = auction.getAuctionNoticeNumber();
		this.city = auction.getCity();
		this.auctioneer = auction.getAuctioneer();
		this.status = auction.getStatus();
		this.normalizedStatus = AuctionStatus.fromSource(auction.getStatus());
		this.statusLabel = this.normalizedStatus.getDescription();
		this.closingDate = auction.getClosingDate();
		this.detranAuctionId = auction.getDetranAuctionId();
		this.auctionYear = auction.getAuctionYear();
		this.providerCode = auction.getProviderCode();
		this.providerName = auction.getProviderName();
		this.stateCode = auction.getStateCode();
		this.stateName = auction.getStateName();
		this.sourceUrl = auction.getSourceUrl();
		this.itemCount = auction.getItems() == null ? 0 : auction.getItems().size();
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

	public AuctionStatus getNormalizedStatus() {
		return normalizedStatus;
	}

	public String getStatusLabel() {
		return statusLabel;
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

	public String getProviderCode() {
		return providerCode;
	}

	public String getProviderName() {
		return providerName;
	}

	public String getStateCode() {
		return stateCode;
	}

	public String getStateName() {
		return stateName;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public Integer getItemCount() {
		return itemCount;
	}
}
