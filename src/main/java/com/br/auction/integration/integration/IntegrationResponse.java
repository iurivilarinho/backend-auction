package com.br.auction.integration.integration;

import java.time.LocalDateTime;
import java.util.List;

import com.br.auction.integration.enums.FetchMode;
import com.br.auction.integration.enums.IntegrationStatus;
import com.br.auction.integration.enums.TriggerMode;
import com.br.auction.integration.mapping.FieldMappingResponse;
import com.br.auction.integration.target.InternalTargetModel;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representacao de uma integracao")
public class IntegrationResponse {

	@Schema(description = "Identificador interno")
	private final Long id;

	@Schema(description = "Codigo unico")
	private final String code;

	@Schema(description = "Nome amigavel")
	private final String name;

	@Schema(description = "Descricao")
	private final String description;

	@Schema(description = "ID da fonte")
	private final Long sourceId;

	@Schema(description = "Codigo da fonte")
	private final String sourceCode;

	@Schema(description = "Nome da fonte")
	private final String sourceName;

	@Schema(description = "Codigo do provedor da fonte")
	private final String providerCode;

	@Schema(description = "ID do modelo da fonte")
	private final Long sourceModelId;

	@Schema(description = "Codigo do modelo da fonte")
	private final String sourceModelCode;

	@Schema(description = "ID da credencial especifica")
	private final Long credentialId;

	@Schema(description = "Codigo da credencial especifica")
	private final String credentialCode;

	@Schema(description = "Modelo interno de destino")
	private final InternalTargetModel targetModel;

	@Schema(description = "Rotulo do modelo interno de destino")
	private final String targetModelLabel;

	@Schema(description = "Modo de disparo")
	private final TriggerMode triggerMode;

	@Schema(description = "Expressao cron")
	private final String cronExpression;

	@Schema(description = "Estrategia de coleta")
	private final FetchMode fetchMode;

	@Schema(description = "Watermark atual")
	private final String watermarkValue;

	@Schema(description = "Tamanho do lote")
	private final Integer batchSize;

	@Schema(description = "Status do ciclo de vida")
	private final IntegrationStatus status;

	@Schema(description = "Descricao do status")
	private final String statusLabel;

	@Schema(description = "Indica se a integracao esta ativa")
	private final Boolean active;

	@Schema(description = "Regras de de->para")
	private final List<FieldMappingResponse> fieldMappings;

	@Schema(description = "Data de criacao")
	private final LocalDateTime createdAt;

	@Schema(description = "Data da ultima atualizacao")
	private final LocalDateTime updatedAt;

	public IntegrationResponse(Integration integration) {
		this.id = integration.getId();
		this.code = integration.getCode();
		this.name = integration.getName();
		this.description = integration.getDescription();
		this.sourceId = integration.getSource() == null ? null : integration.getSource().getId();
		this.sourceCode = integration.getSource() == null ? null : integration.getSource().getCode();
		this.sourceName = integration.getSource() == null ? null : integration.getSource().getName();
		this.providerCode = integration.getSource() == null ? null : integration.getSource().getProviderCode();
		this.sourceModelId = integration.getSourceModel() == null ? null : integration.getSourceModel().getId();
		this.sourceModelCode = integration.getSourceModel() == null ? null : integration.getSourceModel().getCode();
		this.credentialId = integration.getCredential() == null ? null : integration.getCredential().getId();
		this.credentialCode = integration.getCredential() == null ? null : integration.getCredential().getCode();
		this.targetModel = integration.getTargetModel();
		this.targetModelLabel = integration.getTargetModel() == null ? null : integration.getTargetModel().getLabel();
		this.triggerMode = integration.getTriggerMode();
		this.cronExpression = integration.getCronExpression();
		this.fetchMode = integration.getFetchMode();
		this.watermarkValue = integration.getWatermarkValue();
		this.batchSize = integration.getBatchSize();
		this.status = integration.getStatus();
		this.statusLabel = integration.getStatus() == null ? null : integration.getStatus().getDescription();
		this.active = integration.getActive();
		this.fieldMappings = integration.getFieldMappings() == null ? List.of()
				: integration.getFieldMappings().stream().map(FieldMappingResponse::new).toList();
		this.createdAt = integration.getCreatedAt();
		this.updatedAt = integration.getUpdatedAt();
	}

	public Long getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Long getSourceId() {
		return sourceId;
	}

	public String getSourceCode() {
		return sourceCode;
	}

	public String getSourceName() {
		return sourceName;
	}

	public String getProviderCode() {
		return providerCode;
	}

	public Long getSourceModelId() {
		return sourceModelId;
	}

	public String getSourceModelCode() {
		return sourceModelCode;
	}

	public Long getCredentialId() {
		return credentialId;
	}

	public String getCredentialCode() {
		return credentialCode;
	}

	public InternalTargetModel getTargetModel() {
		return targetModel;
	}

	public String getTargetModelLabel() {
		return targetModelLabel;
	}

	public TriggerMode getTriggerMode() {
		return triggerMode;
	}

	public String getCronExpression() {
		return cronExpression;
	}

	public FetchMode getFetchMode() {
		return fetchMode;
	}

	public String getWatermarkValue() {
		return watermarkValue;
	}

	public Integer getBatchSize() {
		return batchSize;
	}

	public IntegrationStatus getStatus() {
		return status;
	}

	public String getStatusLabel() {
		return statusLabel;
	}

	public Boolean getActive() {
		return active;
	}

	public List<FieldMappingResponse> getFieldMappings() {
		return fieldMappings;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
