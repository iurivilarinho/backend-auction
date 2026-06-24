package com.br.auction.response;

import com.br.auction.enums.AuctionProvider;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Provedor de leiloes disponivel para consulta")
public class ProviderResponse {

	@Schema(description = "Codigo do provedor")
	private String code;

	@Schema(description = "Nome do provedor")
	private String name;

	@Schema(description = "Codigo do estado")
	private String stateCode;

	@Schema(description = "Nome do estado")
	private String stateName;

	@Schema(description = "Indica se este e o provedor padrao")
	private Boolean defaultProvider;

	public ProviderResponse(AuctionProvider provider, boolean defaultProvider) {
		this.code = provider.getCode();
		this.name = provider.getName();
		this.stateCode = provider.getStateCode();
		this.stateName = provider.getStateName();
		this.defaultProvider = defaultProvider;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public String getStateCode() {
		return stateCode;
	}

	public String getStateName() {
		return stateName;
	}

	public Boolean getDefaultProvider() {
		return defaultProvider;
	}
}
