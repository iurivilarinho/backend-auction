package com.br.auction.integration.integration;

import com.br.auction.integration.enums.IntegrationStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Solicitacao de transicao de status de uma integracao")
public class IntegrationStatusUpdateRequest {

	@NotNull(message = "O status e obrigatorio")
	@Schema(description = "Novo status desejado")
	private IntegrationStatus status;

	public IntegrationStatus getStatus() {
		return status;
	}

	public void setStatus(IntegrationStatus status) {
		this.status = status;
	}
}
