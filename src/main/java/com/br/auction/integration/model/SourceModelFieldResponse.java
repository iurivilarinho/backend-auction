package com.br.auction.integration.model;

import com.br.auction.integration.enums.FieldDataType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representacao publica de um campo do modelo da fonte")
public class SourceModelFieldResponse {

	@Schema(description = "Identificador interno do campo")
	private Long id;

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

	public SourceModelFieldResponse(SourceModelField field) {
		this.id = field.getId();
		this.code = field.getCode();
		this.name = field.getName();
		this.dataType = field.getDataType();
		this.format = field.getFormat();
		this.required = field.getRequired();
		this.order = field.getOrder();
		this.description = field.getDescription();
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

	public FieldDataType getDataType() {
		return dataType;
	}

	public String getFormat() {
		return format;
	}

	public Boolean getRequired() {
		return required;
	}

	public Integer getOrder() {
		return order;
	}

	public String getDescription() {
		return description;
	}
}
