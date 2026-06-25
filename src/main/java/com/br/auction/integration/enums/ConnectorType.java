package com.br.auction.integration.enums;

/**
 * Tipo de conector usado para coletar dados da fonte externa. O destino e sempre
 * interno (modelos da propria aplicacao), portanto o conector descreve apenas a origem.
 */
public enum ConnectorType {

	REST("REST", "API REST"),
	JDBC("JDBC", "Banco de dados (JDBC)");

	private final String code;
	private final String label;

	ConnectorType(String code, String label) {
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
