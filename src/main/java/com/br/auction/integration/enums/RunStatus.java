package com.br.auction.integration.enums;

/**
 * Status de uma execucao de integracao.
 */
public enum RunStatus {

	RUNNING("Em execucao"),
	SUCCESS("Concluida"),
	PARTIAL("Concluida com falhas"),
	FAILED("Falhou"),
	CANCELLED("Cancelada");

	private final String description;

	RunStatus(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
