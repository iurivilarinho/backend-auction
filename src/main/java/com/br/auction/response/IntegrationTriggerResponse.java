package com.br.auction.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado do disparo manual das integrações agendadas do provedor")
public class IntegrationTriggerResponse {

	@Schema(description = "Indica se ao menos uma integração foi iniciada", example = "true")
	private final boolean started;

	@Schema(description = "Quantidade de integrações disparadas", example = "2")
	private final int integrations;

	@Schema(description = "Mensagem amigável sobre o resultado do disparo")
	private final String message;

	public IntegrationTriggerResponse(int integrations, String message) {
		this.started = integrations > 0;
		this.integrations = integrations;
		this.message = message;
	}

	public boolean isStarted() {
		return started;
	}

	public int getIntegrations() {
		return integrations;
	}

	public String getMessage() {
		return message;
	}
}
