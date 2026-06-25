package com.br.auction.garage.enums;

/**
 * Situacao de um veiculo adquirido em leilao ao longo do seu ciclo de vida na garagem.
 */
public enum AcquisitionStatus {

	ARREMATADO("Arrematado"),
	EM_POSSE("Em posse (busquei)"),
	EM_MANUTENCAO("Em manutencao"),
	ANUNCIADO("Anunciado"),
	VENDIDO("Vendido"),
	FINALIZADO("Finalizado");

	private final String description;

	AcquisitionStatus(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
