package com.br.auction.integration.model;

import java.util.ArrayList;
import java.util.List;

import com.br.auction.integration.enums.ConnectorType;
import com.br.auction.integration.enums.SourceMethod;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados para criacao ou atualizacao de um modelo da fonte")
public class SourceModelRequest {

	@NotBlank(message = "O codigo e obrigatorio")
	@Schema(description = "Codigo unico do modelo da fonte")
	private String code;

	@NotBlank(message = "O nome e obrigatorio")
	@Schema(description = "Nome amigavel do modelo da fonte")
	private String name;

	@Schema(description = "Descricao do modelo da fonte")
	private String description;

	@NotNull(message = "O tipo de conector e obrigatorio")
	@Schema(description = "Tipo de conector compativel com este modelo")
	private ConnectorType connectorType;

	@Schema(description = "Caminho do recurso REST (ex.: api/v1/auctions)")
	private String resourcePath;

	@Schema(description = "Caminho JSON da lista de itens na resposta")
	private String itemsJsonPath;

	@Schema(description = "Caminho JSON do indicador de proxima pagina")
	private String hasNextJsonPath;

	@Schema(description = "Nome do parametro de pagina")
	private String pageParamName;

	@Schema(description = "Nome do parametro de tamanho de pagina")
	private String pageSizeParamName;

	@Schema(description = "Quantidade de registros por pagina")
	private Integer pageSize;

	@Schema(description = "Metodo HTTP de coleta")
	private SourceMethod sourceMethod;

	@Schema(description = "Template do corpo da requisicao para coleta via POST")
	private String requestBodyTemplate;

	@Schema(description = "Nome da tabela/visao para fontes JDBC")
	private String tableName;

	@Schema(description = "Campo que identifica unicamente o registro na fonte (chave de negocio)")
	private String businessKeyField;

	@Schema(description = "Campo usado como watermark para coleta incremental")
	private String watermarkField;

	@Schema(description = "Indica se o modelo esta ativo")
	private Boolean active;

	@Schema(description = "Campos declarados do modelo da fonte")
	private List<SourceModelFieldRequest> fields = new ArrayList<>();

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

	public ConnectorType getConnectorType() {
		return connectorType;
	}

	public void setConnectorType(ConnectorType connectorType) {
		this.connectorType = connectorType;
	}

	public String getResourcePath() {
		return resourcePath;
	}

	public void setResourcePath(String resourcePath) {
		this.resourcePath = resourcePath;
	}

	public String getItemsJsonPath() {
		return itemsJsonPath;
	}

	public void setItemsJsonPath(String itemsJsonPath) {
		this.itemsJsonPath = itemsJsonPath;
	}

	public String getHasNextJsonPath() {
		return hasNextJsonPath;
	}

	public void setHasNextJsonPath(String hasNextJsonPath) {
		this.hasNextJsonPath = hasNextJsonPath;
	}

	public String getPageParamName() {
		return pageParamName;
	}

	public void setPageParamName(String pageParamName) {
		this.pageParamName = pageParamName;
	}

	public String getPageSizeParamName() {
		return pageSizeParamName;
	}

	public void setPageSizeParamName(String pageSizeParamName) {
		this.pageSizeParamName = pageSizeParamName;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	public SourceMethod getSourceMethod() {
		return sourceMethod;
	}

	public void setSourceMethod(SourceMethod sourceMethod) {
		this.sourceMethod = sourceMethod;
	}

	public String getRequestBodyTemplate() {
		return requestBodyTemplate;
	}

	public void setRequestBodyTemplate(String requestBodyTemplate) {
		this.requestBodyTemplate = requestBodyTemplate;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getBusinessKeyField() {
		return businessKeyField;
	}

	public void setBusinessKeyField(String businessKeyField) {
		this.businessKeyField = businessKeyField;
	}

	public String getWatermarkField() {
		return watermarkField;
	}

	public void setWatermarkField(String watermarkField) {
		this.watermarkField = watermarkField;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public List<SourceModelFieldRequest> getFields() {
		return fields;
	}

	public void setFields(List<SourceModelFieldRequest> fields) {
		this.fields = fields;
	}
}
