package com.br.auction.garage.enums;

/**
 * Tipos de gasto associados a um veiculo adquirido em leilao.
 */
public enum ExpenseType {

	ARREMATE("Valor do arremate"),
	LEILOEIRO("Comissao do leiloeiro"),
	DOCUMENTACAO("Documentacao/Despachante"),
	PECAS("Pecas"),
	PINTURA("Pintura"),
	SERVICOS("Servicos/Mao de obra"),
	LAVAGEM("Lavagem/Estetica"),
	FRETE("Frete/Guincho"),
	COMBUSTIVEL("Combustivel"),
	PATIO("Patio/Estadia"),
	OUTROS("Outros");

	private final String description;

	ExpenseType(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
