package com.br.auction.garage.enums;

/**
 * Documentos do veiculo adquirido disponibilizados pelo painel do provedor.
 */
public enum DocumentType {

	CARTA_ARREMATACAO("Carta de arrematacao"),
	EDITAL("Edital"),
	ALVARA_LIBERACAO("Alvara de liberacao");

	private final String description;

	DocumentType(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
