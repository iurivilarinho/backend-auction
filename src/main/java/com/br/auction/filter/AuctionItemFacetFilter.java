package com.br.auction.filter;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Escopo para apuração das facetas de filtro de itens (marcas/anos distintos). Critérios opcionais
 * como atributos, com binding direto dos query params.
 */
@Schema(description = "Escopo das facetas de filtro de itens")
public class AuctionItemFacetFilter {

	@Schema(description = "ID do leilão")
	private Long auctionId;

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
