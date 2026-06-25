package com.br.auction.integration.mapping;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representacao de uma regra de de->para")
public class FieldMappingResponse {

	@Schema(description = "Identificador interno")
	private final Long id;

	@Schema(description = "Campo de origem")
	private final String sourceField;

	@Schema(description = "Campo de destino")
	private final String targetField;

	@Schema(description = "Pipeline de transformacao")
	private final String transform;

	@Schema(description = "Valor padrao")
	private final String defaultValue;

	@Schema(description = "Indica se e obrigatorio")
	private final Boolean required;

	@Schema(description = "Indica se compoe a chave de negocio")
	private final Boolean uniqueKey;

	@Schema(description = "Ordem de aplicacao")
	private final Integer ordem;

	public FieldMappingResponse(FieldMapping mapping) {
		this.id = mapping.getId();
		this.sourceField = mapping.getSourceField();
		this.targetField = mapping.getTargetField();
		this.transform = mapping.getTransform();
		this.defaultValue = mapping.getDefaultValue();
		this.required = mapping.getRequired();
		this.uniqueKey = mapping.getUniqueKey();
		this.ordem = mapping.getOrdem();
	}

	public Long getId() {
		return id;
	}

	public String getSourceField() {
		return sourceField;
	}

	public String getTargetField() {
		return targetField;
	}

	public String getTransform() {
		return transform;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public Boolean getRequired() {
		return required;
	}

	public Boolean getUniqueKey() {
		return uniqueKey;
	}

	public Integer getOrdem() {
		return ordem;
	}
}
