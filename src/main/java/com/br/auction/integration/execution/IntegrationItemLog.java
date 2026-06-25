package com.br.auction.integration.execution;

import java.time.LocalDateTime;

import com.br.auction.integration.enums.ItemStatus;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Log do processamento de um registro individual dentro de uma execucao, util para
 * auditoria e diagnostico (payload de origem, payload de destino e erro).
 */
@Entity
@Table(name = "tbIntegrationItemLog")
@Schema(description = "Log de processamento de um registro durante uma execucao de integracao")
public class IntegrationItemLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Schema(description = "Identificador interno do log")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "fk_Id_Run", foreignKey = @ForeignKey(name = "FK_FROM_TBINTEGRATIONITEMLOG_FOR_TBINTEGRATIONRUN"))
	@Schema(description = "Execucao a qual o log pertence")
	private IntegrationRun run;

	@Column(length = 300)
	@Schema(description = "Chave de negocio do registro")
	private String businessKey;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	@Schema(description = "Status do processamento do registro")
	private ItemStatus status;

	@Column
	@Schema(description = "Duracao do processamento em milissegundos")
	private Long durationMs;

	@Lob
	@Column
	@Schema(description = "Payload original recebido da fonte")
	private String sourcePayload;

	@Lob
	@Column
	@Schema(description = "Payload apos o de->para, gravado no modelo interno")
	private String targetPayload;

	@Lob
	@Column
	@Schema(description = "Mensagem de erro do registro, se houver")
	private String errorMessage;

	@Column(nullable = false)
	@Schema(description = "Data de criacao do log")
	private LocalDateTime createdAt;

	@PrePersist
	public void onCreate() {
		this.createdAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public IntegrationRun getRun() {
		return run;
	}

	public void setRun(IntegrationRun run) {
		this.run = run;
	}

	public String getBusinessKey() {
		return businessKey;
	}

	public void setBusinessKey(String businessKey) {
		this.businessKey = businessKey;
	}

	public ItemStatus getStatus() {
		return status;
	}

	public void setStatus(ItemStatus status) {
		this.status = status;
	}

	public Long getDurationMs() {
		return durationMs;
	}

	public void setDurationMs(Long durationMs) {
		this.durationMs = durationMs;
	}

	public String getSourcePayload() {
		return sourcePayload;
	}

	public void setSourcePayload(String sourcePayload) {
		this.sourcePayload = sourcePayload;
	}

	public String getTargetPayload() {
		return targetPayload;
	}

	public void setTargetPayload(String targetPayload) {
		this.targetPayload = targetPayload;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
