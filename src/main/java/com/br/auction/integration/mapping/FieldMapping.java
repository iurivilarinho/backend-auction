package com.br.auction.integration.mapping;

import com.br.auction.integration.integration.Integration;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Regra de de->para de um campo da fonte para um campo do modelo interno de destino.
 */
@Entity
@Table(name = "tbIntegrationFieldMapping")
@Schema(description = "Mapeamento de um campo da fonte para um campo do modelo interno de destino")
public class FieldMapping {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Schema(description = "Identificador interno do mapeamento")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "fk_Id_Integration", foreignKey = @ForeignKey(name = "FK_FROM_TBINTEGRATIONFIELDMAPPING_FOR_TBINTEGRATION"))
	@Schema(description = "Integracao a qual o mapeamento pertence")
	private Integration integration;

	@Column(nullable = false, length = 300)
	@Schema(description = "Campo de origem (dot-path na fonte)")
	private String sourceField;

	@Column(nullable = false, length = 300)
	@Schema(description = "Campo de destino (campo do modelo interno)")
	private String targetField;

	@Column(length = 200)
	@Schema(description = "Pipeline de transformacao (ex.: TRIM|UPPER|MONEY_BR)")
	private String transform;

	@Column(length = 500)
	@Schema(description = "Valor padrao quando a origem esta vazia")
	private String defaultValue;

	@Column
	@Schema(description = "Indica se o campo e obrigatorio")
	private Boolean required = Boolean.FALSE;

	@Column(name = "uniqueKey")
	@Schema(description = "Indica se o campo compoe a chave de negocio (deduplicacao/upsert)")
	private Boolean uniqueKey = Boolean.FALSE;

	@Column
	@Schema(description = "Ordem de aplicacao do mapeamento")
	private Integer ordem;

	public Long getId() {
		return id;
	}

	public Integration getIntegration() {
		return integration;
	}

	public void setIntegration(Integration integration) {
		this.integration = integration;
	}

	public String getSourceField() {
		return sourceField;
	}

	public void setSourceField(String sourceField) {
		this.sourceField = sourceField;
	}

	public String getTargetField() {
		return targetField;
	}

	public void setTargetField(String targetField) {
		this.targetField = targetField;
	}

	public String getTransform() {
		return transform;
	}

	public void setTransform(String transform) {
		this.transform = transform;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public Boolean getRequired() {
		return required;
	}

	public void setRequired(Boolean required) {
		this.required = required;
	}

	public Boolean getUniqueKey() {
		return uniqueKey;
	}

	public void setUniqueKey(Boolean uniqueKey) {
		this.uniqueKey = uniqueKey;
	}

	public Integer getOrdem() {
		return ordem;
	}

	public void setOrdem(Integer ordem) {
		this.ordem = ordem;
	}
}
