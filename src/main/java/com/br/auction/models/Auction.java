package com.br.auction.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbAuction")
@Schema(description = "Entidade que representa um leilao importado de um provedor externo")
public class Auction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Schema(description = "Identificador interno do leilao")
	private Long id;

	@Column
	@Schema(description = "Numero do edital do leilao")
	private String auctionNoticeNumber;

	@Column
	@Schema(description = "Cidade onde ocorre o leilao")
	private String city;

	@Column
	@Schema(description = "Nome do patio ou leiloeiro")
	private String auctioneer;

	@Column
	@Schema(description = "Status textual do leilao no provedor")
	private String status;

	@Column
	@Schema(description = "Codigo normalizado do provedor de origem")
	private String providerCode;

	@Column
	@Schema(description = "Nome do provedor de origem")
	private String providerName;

	@Column
	@Schema(description = "Codigo do estado do provedor")
	private String stateCode;

	@Column
	@Schema(description = "Nome do estado do provedor")
	private String stateName;

	@Column
	@Schema(description = "Data de encerramento do leilao")
	private LocalDateTime closingDate;

	@Column
	@Schema(description = "Identificador do leilao no portal do provedor")
	private String detranAuctionId;

	@Column
	@Schema(description = "Ano do leilao")
	private String auctionYear;

	@Column
	@Schema(description = "URL publica do leilao no provedor")
	private String sourceUrl;

	@Column(length = 200)
	@Schema(description = "Nome do arquivo do edital baixado")
	private String editalFileName;

	@Column(length = 100)
	@Schema(description = "Content-type do edital baixado")
	private String editalContentType;

	// Base64 como TEXT (LONGVARCHAR): evita Large Object (oid) no Postgres.
	@org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LONGVARCHAR)
	@Column(name = "editalData")
	@Schema(description = "PDF do edital (base64) guardado na base para ficar disponivel offline")
	private String editalData;

	@OneToMany(mappedBy = "auction", cascade = CascadeType.ALL)
	@Schema(description = "Lista de veiculos pertencentes ao leilao")
	private List<AuctionItem> items = new ArrayList<>();

	public Long getId() {
		return id;
	}

	public String getEditalFileName() {
		return editalFileName;
	}

	public void setEditalFileName(String editalFileName) {
		this.editalFileName = editalFileName;
	}

	public String getEditalContentType() {
		return editalContentType;
	}

	public void setEditalContentType(String editalContentType) {
		this.editalContentType = editalContentType;
	}

	public byte[] getEditalBytes() {
		return editalData == null ? new byte[0] : java.util.Base64.getDecoder().decode(editalData);
	}

	public void setEditalBytes(byte[] bytes) {
		this.editalData = bytes == null ? null : java.util.Base64.getEncoder().encodeToString(bytes);
	}

	public boolean hasEdital() {
		return editalData != null && !editalData.isBlank();
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

	public String getProviderCode() {
		return providerCode;
	}

	public void setProviderCode(String providerCode) {
		this.providerCode = providerCode;
	}

	public String getProviderName() {
		return providerName;
	}

	public void setProviderName(String providerName) {
		this.providerName = providerName;
	}

	public String getStateCode() {
		return stateCode;
	}

	public void setStateCode(String stateCode) {
		this.stateCode = stateCode;
	}

	public String getStateName() {
		return stateName;
	}

	public void setStateName(String stateName) {
		this.stateName = stateName;
	}

	public LocalDateTime getClosingDate() {
		return closingDate;
	}

	public void setClosingDate(LocalDateTime closingDate) {
		this.closingDate = closingDate;
	}

	public String getDetranAuctionId() {
		return detranAuctionId;
	}

	public void setDetranAuctionId(String detranAuctionId) {
		this.detranAuctionId = detranAuctionId;
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

	public List<AuctionItem> getItems() {
		return items;
	}

	public void setItems(List<AuctionItem> items) {
		this.items = items;
	}
}
