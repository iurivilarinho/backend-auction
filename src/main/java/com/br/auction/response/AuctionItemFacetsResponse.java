package com.br.auction.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Valores distintos disponiveis para montar os filtros especializados de itens")
public class AuctionItemFacetsResponse {

	@Schema(description = "Marcas distintas disponiveis")
	private final List<String> brands;

	@Schema(description = "Anos distintos disponiveis")
	private final List<String> years;

	public AuctionItemFacetsResponse(List<String> brands, List<String> years) {
		this.brands = brands;
		this.years = years;
	}

	public List<String> getBrands() {
		return brands;
	}

	public List<String> getYears() {
		return years;
	}
}
