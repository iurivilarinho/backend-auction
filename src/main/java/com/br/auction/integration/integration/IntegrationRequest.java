package com.br.auction.integration.integration;

import java.util.ArrayList;
import java.util.List;

import com.br.auction.integration.enums.FetchMode;
import com.br.auction.integration.enums.TriggerMode;
import com.br.auction.integration.mapping.FieldMappingRequest;
import com.br.auction.integration.target.InternalTargetModel;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados para criacao ou atualizacao de uma integracao")
public class IntegrationRequest {

	@NotBlank(message = "O codigo e obrigatorio")
	@Schema(description = "Codigo unico da integracao")
	private String code;

	@NotBlank(message = "O nome e obrigatorio")
	@Schema(description = "Nome amigavel da integracao")
	private String name;

	@Schema(description = "Descricao da integracao")
	private String description;

	@NotNull(message = "A fonte e obrigatoria")
	@Schema(description = "ID da fonte/provedor")
	private Long sourceId;

	@NotNull(message = "O modelo da fonte e obrigatorio")
	@Schema(description = "ID do modelo da fonte")
	private Long sourceModelId;

	@Schema(description = "ID da credencial especifica da integracao (opcional)")
	private Long credentialId;

	@NotNull(message = "O modelo interno de destino e obrigatorio")
	@Schema(description = "Modelo interno de destino (AUCTION ou AUCTION_ITEM)")
	private InternalTargetModel targetModel;

	@Schema(description = "Modo de disparo")
	private TriggerMode triggerMode;

	@Schema(description = "Expressao cron para disparo agendado")
	private String cronExpression;

	@Schema(description = "Estrategia de coleta")
	private FetchMode fetchMode;

	@Schema(description = "Watermark inicial para coleta incremental")
	private String watermarkValue;

	@Schema(description = "Tamanho do lote de processamento")
	private Integer batchSize;

	@Schema(description = "Indica se a integracao esta ativa")
	private Boolean active;

	@Valid
	@Schema(description = "Regras de de->para")
	private List<FieldMappingRequest> fieldMappings = new ArrayList<>();

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Long getSourceId() {
		return sourceId;
	}

	public void setSourceId(Long sourceId) {
		this.sourceId = sourceId;
	}

	public Long getSourceModelId() {
		return sourceModelId;
	}

	public void setSourceModelId(Long sourceModelId) {
		this.sourceModelId = sourceModelId;
	}

	public Long getCredentialId() {
		return credentialId;
	}

	public void setCredentialId(Long credentialId) {
		this.credentialId = credentialId;
	}

	public InternalTargetModel getTargetModel() {
		return targetModel;
	}

	public void setTargetModel(InternalTargetModel targetModel) {
		this.targetModel = targetModel;
	}

	public TriggerMode getTriggerMode() {
		return triggerMode;
	}

	public void setTriggerMode(TriggerMode triggerMode) {
		this.triggerMode = triggerMode;
	}

	public String getCronExpression() {
		return cronExpression;
	}

	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}

	public FetchMode getFetchMode() {
		return fetchMode;
	}

	public void setFetchMode(FetchMode fetchMode) {
		this.fetchMode = fetchMode;
	}

	public String getWatermarkValue() {
		return watermarkValue;
	}

	public void setWatermarkValue(String watermarkValue) {
		this.watermarkValue = watermarkValue;
	}

	public Integer getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(Integer batchSize) {
		this.batchSize = batchSize;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public List<FieldMappingRequest> getFieldMappings() {
		return fieldMappings;
	}

	public void setFieldMappings(List<FieldMappingRequest> fieldMappings) {
		this.fieldMappings = fieldMappings;
	}
}
