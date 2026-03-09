package com.br.leilao.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Resposta do leilão com lotes")
public class AuctionJsonResponse {

	@Schema(description = "Nome do leilão")
	private String auctionName;

	@Schema(description = "Nome do pátio")
	private String yardName;

	@Schema(description = "Quantidade total de páginas")
	private Integer totalPages;

	@Schema(description = "Quantidade de lotes retornados na página")
	private Integer lotsPerPage;

	@Schema(description = "Lista de lotes")
	private List<LotResponse> lots;

	public AuctionJsonResponse() {
	}

	public String getAuctionName() {
		return auctionName;
	}

	public void setAuctionName(String auctionName) {
		this.auctionName = auctionName;
	}

	public String getYardName() {
		return yardName;
	}

	public void setYardName(String yardName) {
		this.yardName = yardName;
	}

	public Integer getTotalPages() {
		return totalPages;
	}

	public void setTotalPages(Integer totalPages) {
		this.totalPages = totalPages;
	}

	public Integer getLotsPerPage() {
		return lotsPerPage;
	}

	public void setLotsPerPage(Integer lotsPerPage) {
		this.lotsPerPage = lotsPerPage;
	}

	public List<LotResponse> getLots() {
		return lots;
	}

	public void setLots(List<LotResponse> lots) {
		this.lots = lots;
	}

}