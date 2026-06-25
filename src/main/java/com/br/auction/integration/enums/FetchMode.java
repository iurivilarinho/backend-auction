package com.br.auction.integration.enums;

/**
 * Estrategia de coleta de dados da fonte.
 */
public enum FetchMode {

	FULL("FULL", "Completa"),
	INCREMENTAL("INCREMENTAL", "Incremental (watermark)");

	private final String code;
	private final String label;

	FetchMode(String code, String label) {
		this.code = code;
		this.label = label;
	}

	public String getCode() {
		return code;
	}

	public String getLabel() {
		return label;
	}
}
