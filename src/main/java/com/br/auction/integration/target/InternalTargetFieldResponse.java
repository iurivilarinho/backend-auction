package com.br.auction.integration.target;

import com.br.auction.integration.enums.FieldDataType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Campo de um modelo interno de destino disponivel para o de->para")
public class InternalTargetFieldResponse {

	@Schema(description = "Codigo do campo de destino")
	private final String code;

	@Schema(description = "Rotulo do campo")
	private final String label;

	@Schema(description = "Tipo de dado")
	private final FieldDataType dataType;

	@Schema(description = "Indica se o campo e obrigatorio")
	private final boolean required;

	@Schema(description = "Indica se o campo e a chave de negocio")
	private final boolean businessKey;

	@Schema(description = "Descricao do campo")
	private final String description;

	public InternalTargetFieldResponse(InternalTargetField field) {
		this.code = field.code();
		this.label = field.label();
		this.dataType = field.dataType();
		this.required = field.required();
		this.businessKey = field.businessKey();
		this.description = field.description();
	}

	public String getCode() {
		return code;
	}

	public String getLabel() {
		return label;
	}

	public FieldDataType getDataType() {
		return dataType;
	}

	public boolean isRequired() {
		return required;
	}

	public boolean isBusinessKey() {
		return businessKey;
	}

	public String getDescription() {
		return description;
	}
}
