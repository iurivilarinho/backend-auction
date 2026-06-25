package com.br.auction.integration.enums;

/**
 * Modo de disparo de uma integracao.
 */
public enum TriggerMode {

	SCHEDULED("SCHEDULED", "Agendada (cron)"),
	MANUAL("MANUAL", "Manual"),
	INBOUND("INBOUND", "Recebimento (inbound)");

	private final String code;
	private final String label;

	TriggerMode(String code, String label) {
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
