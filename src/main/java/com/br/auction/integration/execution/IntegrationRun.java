package com.br.auction.integration.execution;

import java.time.LocalDateTime;

import com.br.auction.integration.enums.RunStatus;
import com.br.auction.integration.enums.TriggerType;
import com.br.auction.integration.integration.Integration;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Registro de uma execucao de integracao, com contadores e janela de watermark.
 */
@Entity
@Table(name = "tbIntegrationRun")
@Schema(description = "Execucao de uma integracao com seus contadores e status")
public class IntegrationRun {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Schema(description = "Identificador interno da execucao")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "fk_Id_Integration", foreignKey = @ForeignKey(name = "FK_FROM_TBINTEGRATIONRUN_FOR_TBINTEGRATION"))
	@Schema(description = "Integracao executada")
	private Integration integration;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	@Schema(description = "Status da execucao")
	private RunStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	@Schema(description = "Origem do disparo")
	private TriggerType triggerType;

	@Column
	@Schema(description = "Inicio da execucao")
	private LocalDateTime startedAt;

	@Column
	@Schema(description = "Fim da execucao")
	private LocalDateTime finishedAt;

	@Column
	@Schema(description = "Duracao em milissegundos")
	private Long durationMs;

	@Column
	@Schema(description = "Total de registros processados")
	private Integer totalRecords = 0;

	@Column
	@Schema(description = "Quantidade de registros gravados com sucesso")
	private Integer successCount = 0;

	@Column
	@Schema(description = "Quantidade de registros com falha")
	private Integer failureCount = 0;

	@Column
	@Schema(description = "Quantidade de registros ignorados")
	private Integer skippedCount = 0;

	@Column(length = 200)
	@Schema(description = "Watermark antes da execucao")
	private String watermarkBefore;

	@Column(length = 200)
	@Schema(description = "Watermark apos a execucao")
	private String watermarkAfter;

	@Lob
	@Column
	@Schema(description = "Mensagem de erro geral da execucao")
	private String errorMessage;

	public Long getId() {
		return id;
	}

	public Integration getIntegration() {
		return integration;
	}

	public void setIntegration(Integration integration) {
		this.integration = integration;
	}

	public RunStatus getStatus() {
		return status;
	}

	public void setStatus(RunStatus status) {
		this.status = status;
	}

	public TriggerType getTriggerType() {
		return triggerType;
	}

	public void setTriggerType(TriggerType triggerType) {
		this.triggerType = triggerType;
	}

	public LocalDateTime getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(LocalDateTime startedAt) {
		this.startedAt = startedAt;
	}

	public LocalDateTime getFinishedAt() {
		return finishedAt;
	}

	public void setFinishedAt(LocalDateTime finishedAt) {
		this.finishedAt = finishedAt;
	}

	public Long getDurationMs() {
		return durationMs;
	}

	public void setDurationMs(Long durationMs) {
		this.durationMs = durationMs;
	}

	public Integer getTotalRecords() {
		return totalRecords;
	}

	public void setTotalRecords(Integer totalRecords) {
		this.totalRecords = totalRecords;
	}

	public Integer getSuccessCount() {
		return successCount;
	}

	public void setSuccessCount(Integer successCount) {
		this.successCount = successCount;
	}

	public Integer getFailureCount() {
		return failureCount;
	}

	public void setFailureCount(Integer failureCount) {
		this.failureCount = failureCount;
	}

	public Integer getSkippedCount() {
		return skippedCount;
	}

	public void setSkippedCount(Integer skippedCount) {
		this.skippedCount = skippedCount;
	}

	public String getWatermarkBefore() {
		return watermarkBefore;
	}

	public void setWatermarkBefore(String watermarkBefore) {
		this.watermarkBefore = watermarkBefore;
	}

	public String getWatermarkAfter() {
		return watermarkAfter;
	}

	public void setWatermarkAfter(String watermarkAfter) {
		this.watermarkAfter = watermarkAfter;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
}
