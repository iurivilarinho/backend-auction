package com.br.auction.enums;

import java.util.Arrays;

public enum AuctionProvider {

	DETRAN_MG("DETRAN_MG", "DETRAN Minas Gerais", "MG", "Minas Gerais", "https://leilao.detran.mg.gov.br"),
	// Plataforma Leilo (Grupo Leilo), leiloeiro de GO. API publica em api.leilo.com.br (ver LeiloService).
	LEILO_GO("LEILO_GO", "Leilo (Grupo Leilo)", "GO", "Goias", "https://api.leilo.com.br");

	private final String code;
	private final String name;
	private final String stateCode;
	private final String stateName;
	private final String baseUrl;

	AuctionProvider(String code, String name, String stateCode, String stateName, String baseUrl) {
		this.code = code;
		this.name = name;
		this.stateCode = stateCode;
		this.stateName = stateName;
		this.baseUrl = baseUrl;
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

	public String getBaseUrl() {
		return baseUrl;
	}

	public static AuctionProvider defaultProvider() {
		return DETRAN_MG;
	}

	public static AuctionProvider fromCodeOrDefault(String code) {
		if (code == null || code.isBlank()) {
			return defaultProvider();
		}

		return Arrays.stream(values())
				.filter(provider -> provider.code.equalsIgnoreCase(code.trim()))
				.findFirst()
				.orElse(defaultProvider());
	}
}
