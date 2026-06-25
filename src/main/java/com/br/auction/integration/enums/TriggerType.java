package com.br.auction.integration.enums;

/**
 * Origem do disparo de uma execucao.
 */
public enum TriggerType {

	SCHEDULED("Agendada"),
	MANUAL("Manual"),
	INBOUND("Recebimento");

	private final String description;

	TriggerType(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
