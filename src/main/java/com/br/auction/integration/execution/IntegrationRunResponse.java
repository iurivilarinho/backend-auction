package com.br.auction.integration.execution;

import java.time.LocalDateTime;

import com.br.auction.integration.enums.RunStatus;
import com.br.auction.integration.enums.TriggerType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representacao de uma execucao de integracao")
public class IntegrationRunResponse {

	@Schema(description = "Identificador interno")
	private final Long id;

	@Schema(description = "ID da integracao")
	private final Long integrationId;

	@Schema(description = "Codigo da integracao")
	private final String integrationCode;

	@Schema(description = "Status da execucao")
	private final RunStatus status;

	@Schema(description = "Descricao do status")
	private final String statusLabel;

	@Schema(description = "Origem do disparo")
	private final TriggerType triggerType;

	@Schema(description = "Inicio")
	private final LocalDateTime startedAt;

	@Schema(description = "Fim")
	private final LocalDateTime finishedAt;

	@Schema(description = "Duracao em milissegundos")
	private final Long durationMs;

	@Schema(description = "Total de registros")
	private final Integer totalRecords;

	@Schema(description = "Sucessos")
	private final Integer successCount;

	@Schema(description = "Falhas")
	private final Integer failureCount;

	@Schema(description = "Ignorados")
	private final Integer skippedCount;

	@Schema(description = "Watermark antes")
	private final String watermarkBefore;

	@Schema(description = "Watermark depois")
	private final String watermarkAfter;

	@Schema(description = "Mensagem de erro")
	private final String errorMessage;

	public IntegrationRunResponse(IntegrationRun run) {
		this.id = run.getId();
		this.integrationId = run.getIntegration() == null ? null : run.getIntegration().getId();
		this.integrationCode = run.getIntegration() == null ? null : run.getIntegration().getCode();
		this.status = run.getStatus();
		this.statusLabel = run.getStatus() == null ? null : run.getStatus().getDescription();
		this.triggerType = run.getTriggerType();
		this.startedAt = run.getStartedAt();
		this.finishedAt = run.getFinishedAt();
		this.durationMs = run.getDurationMs();
		this.totalRecords = run.getTotalRecords();
		this.successCount = run.getSuccessCount();
		this.failureCount = run.getFailureCount();
		this.skippedCount = run.getSkippedCount();
		this.watermarkBefore = run.getWatermarkBefore();
		this.watermarkAfter = run.getWatermarkAfter();
		this.errorMessage = run.getErrorMessage();
	}

	public Long getId() {
		return id;
	}

	public Long getIntegrationId() {
		return integrationId;
	}

	public String getIntegrationCode() {
		return integrationCode;
	}

	public RunStatus getStatus() {
		return status;
	}

	public String getStatusLabel() {
		return statusLabel;
	}

	public TriggerType getTriggerType() {
		return triggerType;
	}

	public LocalDateTime getStartedAt() {
		return startedAt;
	}

	public LocalDateTime getFinishedAt() {
		return finishedAt;
	}

	public Long getDurationMs() {
		return durationMs;
	}

	public Integer getTotalRecords() {
		return totalRecords;
	}

	public Integer getSuccessCount() {
		return successCount;
	}

	public Integer getFailureCount() {
		return failureCount;
	}

	public Integer getSkippedCount() {
		return skippedCount;
	}

	public String getWatermarkBefore() {
		return watermarkBefore;
	}

	public String getWatermarkAfter() {
		return watermarkAfter;
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}
