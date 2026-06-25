package com.br.auction.garage.enums;

/**
 * Documentos do veiculo adquirido disponibilizados pelo painel do provedor.
 */
public enum DocumentType {

	CARTA_ARREMATACAO("Carta de arrematacao"),
	NOTA_ARREMATACAO("Nota de arrematacao"),
	ALVARA_LIBERACAO("Alvara de liberacao"),
	EDITAL("Edital");

	private final String description;

	DocumentType(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
