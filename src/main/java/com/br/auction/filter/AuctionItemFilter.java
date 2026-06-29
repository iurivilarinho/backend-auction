package com.br.auction.filter;

import java.math.BigDecimal;
import java.util.List;

import com.br.auction.enums.AuctionStatus;
import com.br.auction.enums.LotType;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Filtros de busca de itens de leilão. Os campos são os critérios opcionais como atributos — o
 * Spring faz o binding direto dos query params, sem lista extensa de {@code @RequestParam} no
 * controller. Critérios nulos/vazios são ignorados ao montar a {@code Specification}.
 */
@Schema(description = "Filtros de busca de itens de leilão")
public class AuctionItemFilter {

	@Schema(description = "ID do leilão")
	private Long auctionId;

	@Schema(description = "Status do leilão")
	private List<AuctionStatus> auctionStatus;

	@Schema(description = "Tipo do lote")
	private List<LotType> type;

	@Schema(description = "Busca textual")
	private String search;

	@Schema(description = "Marcas (uma ou mais)")
	private List<String> brand;

	@Schema(description = "Anos (um ou mais)")
	private List<String> year;

	@Schema(description = "Modelo (contém)")
	private String model;

	@Schema(description = "Valor mínimo do lance")
	private BigDecimal minBid;

	@Schema(description = "Valor máximo do lance")
	private BigDecimal maxBid;

	@Schema(description = "Valor FIPE mínimo")
	private BigDecimal minFipe;

	@Schema(description = "Valor FIPE máximo")
	private BigDecimal maxFipe;

	@Schema(description = "Filtra por lances encerrados (true) ou não encerrados (false)")
	private Boolean closed;

	@Schema(description = "Códigos dos provedores (um ou mais)")
	private List<String> providerCode;

	@Schema(description = "Código do estado")
	private String stateCode;

	public Long getAuctionId() {
		return auctionId;
	}

	public void setAuctionId(Long auctionId) {
		this.auctionId = auctionId;
	}

	public List<AuctionStatus> getAuctionStatus() {
		return auctionStatus;
	}

	public void setAuctionStatus(List<AuctionStatus> auctionStatus) {
		this.auctionStatus = auctionStatus;
	}

	public List<LotType> getType() {
		return type;
	}

	public void setType(List<LotType> type) {
		this.type = type;
	}

	public String getSearch() {
		return search;
	}

	public void setSearch(String search) {
		this.search = search;
	}

	public List<String> getBrand() {
		return brand;
	}

	public void setBrand(List<String> brand) {
		this.brand = brand;
	}

	public List<String> getYear() {
		return year;
	}

	public void setYear(List<String> year) {
		this.year = year;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public BigDecimal getMinBid() {
		return minBid;
	}

	public void setMinBid(BigDecimal minBid) {
		this.minBid = minBid;
	}

	public BigDecimal getMaxBid() {
		return maxBid;
	}

	public void setMaxBid(BigDecimal maxBid) {
		this.maxBid = maxBid;
	}

	public BigDecimal getMinFipe() {
		return minFipe;
	}

	public void setMinFipe(BigDecimal minFipe) {
		this.minFipe = minFipe;
	}

	public BigDecimal getMaxFipe() {
		return maxFipe;
	}

	public void setMaxFipe(BigDecimal maxFipe) {
		this.maxFipe = maxFipe;
	}

	public Boolean getClosed() {
		return closed;
	}

	public void setClosed(Boolean closed) {
		this.closed = closed;
	}

	public List<String> getProviderCode() {
		return providerCode;
	}

	public void setProviderCode(List<String> providerCode) {
		this.providerCode = providerCode;
	}

	public String getStateCode() {
		return stateCode;
	}

	public void setStateCode(String stateCode) {
		this.stateCode = stateCode;
	}
}
