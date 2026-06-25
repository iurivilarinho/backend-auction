package com.br.auction.integration.model;

import com.br.auction.integration.enums.FieldDataType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados de um campo declarado do modelo da fonte")
public class SourceModelFieldRequest {

	@Schema(description = "Codigo/caminho do campo na fonte (dot-path)")
	private String code;

	@Schema(description = "Nome de exibicao do campo")
	private String name;

	@Schema(description = "Tipo de dado do campo")
	private FieldDataType dataType;

	@Schema(description = "Formato auxiliar (ex.: dd/MM/yyyy)")
	private String format;

	@Schema(description = "Indica se o campo e obrigatorio na fonte")
	private Boolean required;

	@Schema(description = "Ordem de exibicao do campo")
	private Integer order;

	@Schema(description = "Descricao do campo")
	private String description;

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

	public FieldDataType getDataType() {
		return dataType;
	}

	public void setDataType(FieldDataType dataType) {
		this.dataType = dataType;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public Boolean getRequired() {
		return required;
	}

	public void setRequired(Boolean required) {
		this.required = required;
	}

	public Integer getOrder() {
		return order;
	}

	public void setOrder(Integer order) {
		this.order = order;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
