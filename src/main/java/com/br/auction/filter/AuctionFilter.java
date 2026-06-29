package com.br.auction.filter;

import java.util.List;

import com.br.auction.enums.AuctionStatus;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Filtros de busca de leilões. Os campos são os critérios opcionais como atributos — o Spring faz o
 * binding direto dos query params, sem lista de {@code @RequestParam} no controller.
 */
@Schema(description = "Filtros de busca de leilões")
public class AuctionFilter {

	@Schema(description = "Status normalizado (um ou mais)")
	private List<AuctionStatus> status;

	@Schema(description = "Busca textual")
	private String search;

	@Schema(description = "Códigos dos provedores (um ou mais)")
	private List<String> providerCode;

	@Schema(description = "Código do estado")
	private String stateCode;

	public List<AuctionStatus> getStatus() {
		return status;
	}

	public void setStatus(List<AuctionStatus> status) {
		this.status = status;
	}

	public String getSearch() {
		return search;
	}

	public void setSearch(String search) {
		this.search = search;
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
