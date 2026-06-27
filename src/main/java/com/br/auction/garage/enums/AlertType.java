package com.br.auction.garage.enums;

/**
 * Tipos de alerta suportados pelo motor de regras. O tipo determina qual gatilho e avaliado e
 * quais parametros do {@code VehicleAlert} sao usados. Tudo dirigido por dados — nenhum criterio
 * fica chumbado no codigo.
 */
public enum AlertType {

	/** Surgiu um lote novo que combina com o criterio (palavra-chave/cidade/tipo/raio). */
	NEW_MATCH("Novo veiculo correspondente"),

	/** Um lote vigiado esta perto de encerrar (faltam <= leadTimeMinutes para o closingDate). */
	CLOSING_SOON("Encerrando em breve"),

	/** O lance atual de um lote vigiado passou do teto (thresholdValue). */
	PRICE_ABOVE("Lance passou do teto"),

	/** Um lote ja encerrado foi arrematado por valor <= alvo (thresholdValue). */
	SOLD_BELOW("Arrematado abaixo do alvo"),

	/** Lote com lance <= fipePercent% da tabela FIPE (barganha). */
	FIPE_DEAL("Barganha FIPE");

	private final String description;

	AlertType(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
