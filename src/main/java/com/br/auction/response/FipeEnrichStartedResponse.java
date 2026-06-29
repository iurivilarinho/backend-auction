package com.br.auction.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Confirmação de que a busca FIPE em segundo plano foi iniciada")
public class FipeEnrichStartedResponse {

	@Schema(description = "Indica se o enriquecimento FIPE foi iniciado", example = "true")
	private final boolean started;

	@Schema(description = "Mensagem amigável sobre o início do processamento")
	private final String message;

	public FipeEnrichStartedResponse(boolean started, String message) {
		this.started = started;
		this.message = message;
	}

	public boolean isStarted() {
		return started;
	}

	public String getMessage() {
		return message;
	}
}
