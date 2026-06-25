package com.br.auction.integration.enums;

/**
 * Status do processamento de um registro individual durante uma execucao.
 */
public enum ItemStatus {

	SUCCESS("Sucesso"),
	FAILED("Falha"),
	SKIPPED("Ignorado");

	private final String description;

	ItemStatus(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
