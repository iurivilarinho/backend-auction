package com.br.auction.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado do enfileiramento da geocodificação das cidades dos leilões")
public class GeocodingWarmupResponse {

	@Schema(description = "Quantidade de cidades enfileiradas para geocodificação", example = "12")
	private final int queued;

	@Schema(description = "Mensagem amigável sobre o aquecimento")
	private final String message;

	public GeocodingWarmupResponse(int queued, String message) {
		this.queued = queued;
		this.message = message;
	}

	public int getQueued() {
		return queued;
	}

	public String getMessage() {
		return message;
	}
}
