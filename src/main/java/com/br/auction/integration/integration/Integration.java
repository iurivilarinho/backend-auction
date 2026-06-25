package com.br.auction.integration.integration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.br.auction.integration.credential.Credential;
import com.br.auction.integration.enums.FetchMode;
import com.br.auction.integration.enums.IntegrationStatus;
import com.br.auction.integration.enums.TriggerMode;
import com.br.auction.integration.mapping.FieldMapping;
import com.br.auction.integration.model.SourceModel;
import com.br.auction.integration.source.IntegrationSource;
import com.br.auction.integration.target.InternalTargetModel;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Integracao configurada entre uma fonte e um modelo interno da aplicacao. Orquestra a
 * coleta (fonte + modelo da fonte), o de->para (mappings) e a persistencia no modelo
 * interno de destino (sempre interno).
 */
@Entity
@Table(name = "tbIntegration")
@Schema(description = "Integracao que coleta dados de uma fonte e os grava em um modelo interno da aplicacao")
public class Integration {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Schema(description = "Identificador interno da integracao")
	private Long id;

	@Column(nullable = false, unique = true, length = 80)
	@Schema(description = "Codigo unico da integracao")
	private String code;

	@Column(nullable = false, length = 200)
	@Schema(description = "Nome amigavel da integracao")
	private String name;

	@Column(length = 1000)
	@Schema(description = "Descricao da integracao")
	private String description;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "fk_Id_Source", foreignKey = @ForeignKey(name = "FK_FROM_TBINTEGRATION_FOR_TBINTEGRATIONSOURCE"))
	@Schema(description = "Fonte/provedor de dados")
	private IntegrationSource source;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "fk_Id_SourceModel", foreignKey = @ForeignKey(name = "FK_FROM_TBINTEGRATION_FOR_TBINTEGRATIONSOURCEMODEL"))
	@Schema(description = "Modelo de dados da fonte")
	private SourceModel sourceModel;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "fk_Id_Credential", foreignKey = @ForeignKey(name = "FK_FROM_TBINTEGRATION_FOR_TBINTEGRATIONCREDENTIAL"))
	@Schema(description = "Credencial especifica da integracao (sobrepoe a credencial da fonte, se informada)")
	private Credential credential;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	@Schema(description = "Modelo interno de destino (AUCTION ou AUCTION_ITEM)")
	private InternalTargetModel targetModel;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	@Schema(description = "Modo de disparo da integracao")
	private TriggerMode triggerMode = TriggerMode.MANUAL;

	@Column(length = 100)
	@Schema(description = "Expressao cron para disparo agendado")
	private String cronExpression;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	@Schema(description = "Estrategia de coleta")
	private FetchMode fetchMode = FetchMode.FULL;

	@Column(length = 200)
	@Schema(description = "Ultimo watermark persistido para coleta incremental")
	private String watermarkValue;

	@Column
	@Schema(description = "Tamanho do lote de processamento")
	private Integer batchSize = 100;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	@Schema(description = "Status do ciclo de vida da integracao")
	private IntegrationStatus status = IntegrationStatus.DRAFT;

	@Column(nullable = false)
	@Schema(description = "Indica se a integracao esta ativa")
	private Boolean active = Boolean.TRUE;

	@OneToMany(mappedBy = "integration", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("ordem ASC")
	@Schema(description = "Regras de de->para da fonte para o modelo interno")
	private List<FieldMapping> fieldMappings = new ArrayList<>();

	@Column(nullable = false)
	@Schema(description = "Data de criacao")
	private LocalDateTime createdAt;

	@Column(nullable = false)
	@Schema(description = "Data da ultima atualizacao")
	private LocalDateTime updatedAt;

	@PrePersist
	public void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
		applyDefaults();
	}

	@PreUpdate
	public void onUpdate() {
		this.updatedAt = LocalDateTime.now();
		applyDefaults();
	}

	private void applyDefaults() {
		if (this.triggerMode == null) {
			this.triggerMode = TriggerMode.MANUAL;
		}
		if (this.fetchMode == null) {
			this.fetchMode = FetchMode.FULL;
		}
		if (this.status == null) {
			this.status = IntegrationStatus.DRAFT;
		}
		if (this.batchSize == null || this.batchSize <= 0) {
			this.batchSize = 100;
		}
		if (this.active == null) {
			this.active = Boolean.TRUE;
		}
	}

	public Long getId() {
		return id;
	}

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

	public IntegrationSource getSource() {
		return source;
	}

	public void setSource(IntegrationSource source) {
		this.source = source;
	}

	public SourceModel getSourceModel() {
		return sourceModel;
	}

	public void setSourceModel(SourceModel sourceModel) {
		this.sourceModel = sourceModel;
	}

	public Credential getCredential() {
		return credential;
	}

	public void setCredential(Credential credential) {
		this.credential = credential;
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

	public IntegrationStatus getStatus() {
		return status;
	}

	public void setStatus(IntegrationStatus status) {
		this.status = status;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public List<FieldMapping> getFieldMappings() {
		return fieldMappings;
	}

	public void setFieldMappings(List<FieldMapping> fieldMappings) {
		this.fieldMappings = fieldMappings;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
