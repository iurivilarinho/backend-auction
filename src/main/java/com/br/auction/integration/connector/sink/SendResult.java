package com.br.auction.integration.connector.sink;

/**
 * Resultado da gravacao de um registro no modelo interno de destino.
 */
public record SendResult(boolean success, boolean createdNew, String errorMessage) {

	public static SendResult created() {
		return new SendResult(true, true, null);
	}

	public static SendResult updated() {
		return new SendResult(true, false, null);
	}

	public static SendResult failed(String reason) {
		return new SendResult(false, false, reason);
	}
}
