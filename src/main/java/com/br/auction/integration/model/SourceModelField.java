package com.br.auction.integration.model;

import com.br.auction.integration.enums.FieldDataType;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbIntegrationSourceModelField")
@Schema(description = "Campo declarado de um modelo da fonte. Documenta o contrato e alimenta o editor de de->para.")
public class SourceModelField {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Schema(description = "Identificador interno do campo")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "fk_Id_SourceModel", foreignKey = @ForeignKey(name = "FK_FROM_TBINTEGRATIONSOURCEMODELFIELD_FOR_TBINTEGRATIONSOURCEMODEL"))
	@Schema(description = "Modelo da fonte ao qual o campo pertence")
	private SourceModel sourceModel;

	@Column(nullable = false, length = 200)
	@Schema(description = "Codigo/caminho do campo na fonte (dot-path)")
	private String code;

	@Column(length = 200)
	@Schema(description = "Nome de exibicao do campo")
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	@Schema(description = "Tipo de dado do campo")
	private FieldDataType dataType;

	@Column(length = 100)
	@Schema(description = "Formato auxiliar (ex.: dd/MM/yyyy)")
	private String format;

	@Column
	@Schema(description = "Indica se o campo e obrigatorio na fonte")
	private Boolean required = Boolean.FALSE;

	@Column(name = "fieldOrder")
	@Schema(description = "Ordem de exibicao do campo")
	private Integer order;

	@Column(length = 500)
	@Schema(description = "Descricao do campo")
	private String description;

	public Long getId() {
		return id;
	}

	public SourceModel getSourceModel() {
		return sourceModel;
	}

	public void setSourceModel(SourceModel sourceModel) {
		this.sourceModel = sourceModel;
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
