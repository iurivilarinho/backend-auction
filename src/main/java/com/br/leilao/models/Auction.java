package com.br.leilao.models;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

@Entity
@Table(name = "tbAuction")
@Schema(description = "Entidade que representa um leilão do DETRAN")
public class Auction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Schema(description = "Identificador interno do leilão")
	private Long id;

	@Column
	@Schema(description = "Número do edital do leilão")
	private String auctionNoticeNumber;

	@Column
	@Schema(description = "Cidade onde ocorre o leilão")
	private String city;

	@Column
	@Schema(description = "Nome do pátio ou leiloeiro")
	private String auctioneer;

	@Column
	@Schema(description = "Status do leilão")
	private String status;

	@Column
	@Schema(description = "Data de encerramento do leilão")
	private LocalDateTime closingDate;

	@Column
	@Schema(description = "Identificador do leilão no portal do DETRAN")
	private String detranAuctionId;

	@Column
	@Schema(description = "Ano do leilão")
	private String auctionYear;

	@OneToMany(mappedBy = "auction", cascade = CascadeType.ALL)
	@Schema(description = "Lista de veículos pertencentes ao leilão")
	private List<AuctionItem> items;

	public Long getId() {
		return id;
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

	public List<AuctionItem> getItems() {
		return items;
	}

	public void setItems(List<AuctionItem> items) {
		this.items = items;
	}
}