package com.br.auction.filter;

import java.util.List;

import com.br.auction.enums.PriceStatGroupBy;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Filtros das estatísticas de preço de arremate. Critérios opcionais como atributos, com binding
 * direto dos query params (sem lista de {@code @RequestParam} no controller).
 */
@Schema(description = "Filtros das estatísticas de preço de arremate")
public class PriceStatFilter {

	@Schema(description = "Granularidade do agrupamento", defaultValue = "BRAND_MODEL_YEAR")
	private PriceStatGroupBy groupBy = PriceStatGroupBy.BRAND_MODEL_YEAR;

	@Schema(description = "Filtrar por uma ou mais marcas")
	private List<String> brand;

	@Schema(description = "Códigos dos provedores (um ou mais)")
	private List<String> providerCode;

	@Schema(description = "Código do estado")
	private String stateCode;

	public PriceStatGroupBy getGroupBy() {
		return groupBy;
	}

	public void setGroupBy(PriceStatGroupBy groupBy) {
		this.groupBy = groupBy;
	}

	public List<String> getBrand() {
		return brand;
	}

	public void setBrand(List<String> brand) {
		this.brand = brand;
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
