package com.br.auction.integration.execution;

import java.time.LocalDateTime;

import com.br.auction.integration.enums.ItemStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Log de processamento de um registro durante uma execucao")
public class IntegrationItemLogResponse {

	@Schema(description = "Identificador interno")
	private final Long id;

	@Schema(description = "Chave de negocio do registro")
	private final String businessKey;

	@Schema(description = "Status do processamento")
	private final ItemStatus status;

	@Schema(description = "Descricao do status")
	private final String statusLabel;

	@Schema(description = "Duracao em milissegundos")
	private final Long durationMs;

	@Schema(description = "Payload de origem")
	private final String sourcePayload;

	@Schema(description = "Payload de destino")
	private final String targetPayload;

	@Schema(description = "Mensagem de erro")
	private final String errorMessage;

	@Schema(description = "Data de criacao")
	private final LocalDateTime createdAt;

	public IntegrationItemLogResponse(IntegrationItemLog log) {
		this.id = log.getId();
		this.businessKey = log.getBusinessKey();
		this.status = log.getStatus();
		this.statusLabel = log.getStatus() == null ? null : log.getStatus().getDescription();
		this.durationMs = log.getDurationMs();
		this.sourcePayload = log.getSourcePayload();
		this.targetPayload = log.getTargetPayload();
		this.errorMessage = log.getErrorMessage();
		this.createdAt = log.getCreatedAt();
	}

	public Long getId() {
		return id;
	}

	public String getBusinessKey() {
		return businessKey;
	}

	public ItemStatus getStatus() {
		return status;
	}

	public String getStatusLabel() {
		return statusLabel;
	}

	public Long getDurationMs() {
		return durationMs;
	}

	public String getSourcePayload() {
		return sourcePayload;
	}

	public String getTargetPayload() {
		return targetPayload;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
