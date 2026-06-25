package com.br.auction.integration.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.br.auction.integration.enums.ConnectorType;
import com.br.auction.integration.enums.SourceMethod;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representacao publica de um modelo da fonte")
public class SourceModelResponse {

	@Schema(description = "Identificador interno")
	private Long id;

	@Schema(description = "Codigo unico")
	private String code;

	@Schema(description = "Nome amigavel")
	private String name;

	@Schema(description = "Descricao do modelo da fonte")
	private String description;

	@Schema(description = "Tipo de conector compativel com este modelo")
	private ConnectorType connectorType;

	@Schema(description = "Descricao do tipo de conector")
	private String connectorTypeLabel;

	@Schema(description = "Caminho do recurso REST")
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
	private List<SourceModelFieldResponse> fields;

	@Schema(description = "Quantidade de campos declarados")
	private int fieldCount;

	@Schema(description = "Data de criacao")
	private LocalDateTime createdAt;

	@Schema(description = "Data da ultima atualizacao")
	private LocalDateTime updatedAt;

	public SourceModelResponse(SourceModel model) {
		this.id = model.getId();
		this.code = model.getCode();
		this.name = model.getName();
		this.description = model.getDescription();
		this.connectorType = model.getConnectorType();
		this.connectorTypeLabel = model.getConnectorType() == null ? null : model.getConnectorType().getLabel();
		this.resourcePath = model.getResourcePath();
		this.itemsJsonPath = model.getItemsJsonPath();
		this.hasNextJsonPath = model.getHasNextJsonPath();
		this.pageParamName = model.getPageParamName();
		this.pageSizeParamName = model.getPageSizeParamName();
		this.pageSize = model.getPageSize();
		this.sourceMethod = model.getSourceMethod();
		this.requestBodyTemplate = model.getRequestBodyTemplate();
		this.tableName = model.getTableName();
		this.businessKeyField = model.getBusinessKeyField();
		this.watermarkField = model.getWatermarkField();
		this.active = model.getActive();
		this.fields = model.getFields() == null ? List.of()
				: model.getFields().stream().map(SourceModelFieldResponse::new).collect(Collectors.toList());
		this.fieldCount = this.fields.size();
		this.createdAt = model.getCreatedAt();
		this.updatedAt = model.getUpdatedAt();
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

	public String getDescription() {
		return description;
	}

	public ConnectorType getConnectorType() {
		return connectorType;
	}

	public String getConnectorTypeLabel() {
		return connectorTypeLabel;
	}

	public String getResourcePath() {
		return resourcePath;
	}

	public String getItemsJsonPath() {
		return itemsJsonPath;
	}

	public String getHasNextJsonPath() {
		return hasNextJsonPath;
	}

	public String getPageParamName() {
		return pageParamName;
	}

	public String getPageSizeParamName() {
		return pageSizeParamName;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public SourceMethod getSourceMethod() {
		return sourceMethod;
	}

	public String getRequestBodyTemplate() {
		return requestBodyTemplate;
	}

	public String getTableName() {
		return tableName;
	}

	public String getBusinessKeyField() {
		return businessKeyField;
	}

	public String getWatermarkField() {
		return watermarkField;
	}

	public Boolean getActive() {
		return active;
	}

	public List<SourceModelFieldResponse> getFields() {
		return fields;
	}

	public int getFieldCount() {
		return fieldCount;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
