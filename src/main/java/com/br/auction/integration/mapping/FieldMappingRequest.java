package com.br.auction.integration.mapping;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Regra de de->para de um campo da fonte para o modelo interno")
public class FieldMappingRequest {

	@NotBlank(message = "O campo de origem e obrigatorio")
	@Schema(description = "Campo de origem (dot-path na fonte)")
	private String sourceField;

	@NotBlank(message = "O campo de destino e obrigatorio")
	@Schema(description = "Campo de destino (campo do modelo interno)")
	private String targetField;

	@Schema(description = "Pipeline de transformacao (ex.: TRIM|UPPER|MONEY_BR)")
	private String transform;

	@Schema(description = "Valor padrao quando a origem esta vazia")
	private String defaultValue;

	@Schema(description = "Indica se o campo e obrigatorio")
	private Boolean required;

	@Schema(description = "Indica se o campo compoe a chave de negocio")
	private Boolean uniqueKey;

	@Schema(description = "Ordem de aplicacao")
	private Integer ordem;

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
