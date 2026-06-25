package com.br.auction.integration.enums;

/**
 * Tipo de dado de um campo da fonte ou do modelo interno de destino. Usado para
 * documentar o contrato e converter valores durante o de->para.
 */
public enum FieldDataType {

	STRING("STRING", "Texto"),
	INTEGER("INTEGER", "Inteiro"),
	LONG("LONG", "Inteiro longo"),
	DECIMAL("DECIMAL", "Decimal"),
	BOOLEAN("BOOLEAN", "Booleano"),
	DATE("DATE", "Data"),
	DATETIME("DATETIME", "Data e hora");

	private final String code;
	private final String label;

	FieldDataType(String code, String label) {
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
