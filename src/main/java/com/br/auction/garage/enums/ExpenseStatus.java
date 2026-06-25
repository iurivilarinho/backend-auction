package com.br.auction.garage.enums;

/**
 * Situacao de um gasto: pode comecar como cotacao (varios orcamentos) e so virar despesa
 * efetiva quando um dos orcamentos for marcado como comprado.
 */
public enum ExpenseStatus {

	COTACAO("Em cotacao"),
	COMPRADO("Comprado");

	private final String description;

	ExpenseStatus(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
