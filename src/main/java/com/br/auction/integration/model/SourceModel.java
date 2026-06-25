package com.br.auction.integration.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.br.auction.integration.enums.ConnectorType;
import com.br.auction.integration.enums.SourceMethod;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Descreve o formato dos registros entregues por uma fonte: caminho do recurso,
 * paginacao, chave de negocio, campo de watermark e os campos declarados. O destino e
 * sempre interno, portanto este modelo cobre apenas a origem.
 */
@Entity
@Table(name = "tbIntegrationSourceModel")
@Schema(description = "Modelo de dados da fonte: estrutura dos registros recebidos do provedor")
public class SourceModel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Schema(description = "Identificador interno do modelo da fonte")
	private Long id;

	@Column(nullable = false, unique = true, length = 80)
	@Schema(description = "Codigo unico do modelo da fonte")
	private String code;

	@Column(nullable = false, length = 200)
	@Schema(description = "Nome amigavel do modelo da fonte")
	private String name;

	@Column(length = 1000)
	@Schema(description = "Descricao do modelo da fonte")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	@Schema(description = "Tipo de conector compativel com este modelo")
	private ConnectorType connectorType;

	@Column(length = 500)
	@Schema(description = "Caminho do recurso REST (ex.: api/v1/auctions)")
	private String resourcePath;

	@Column(length = 200)
	@Schema(description = "Caminho JSON da lista de itens na resposta (padrao: items)")
	private String itemsJsonPath = "items";

	@Column(length = 200)
	@Schema(description = "Caminho JSON do indicador de proxima pagina (padrao: hasNext)")
	private String hasNextJsonPath = "hasNext";

	@Column(length = 80)
	@Schema(description = "Nome do parametro de pagina (padrao: page)")
	private String pageParamName = "page";

	@Column(length = 80)
	@Schema(description = "Nome do parametro de tamanho de pagina (padrao: pageSize)")
	private String pageSizeParamName = "pageSize";

	@Column
	@Schema(description = "Quantidade de registros por pagina (padrao: 100)")
	private Integer pageSize = 100;

	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	@Schema(description = "Metodo HTTP de coleta (padrao: GET)")
	private SourceMethod sourceMethod = SourceMethod.GET;

	@Column(columnDefinition = "TEXT")
	@Schema(description = "Template do corpo da requisicao para coleta via POST")
	private String requestBodyTemplate;

	@Column(length = 200)
	@Schema(description = "Nome da tabela/visao para fontes JDBC")
	private String tableName;

	@Column(length = 200)
	@Schema(description = "Campo que identifica unicamente o registro na fonte (chave de negocio)")
	private String businessKeyField;

	@Column(length = 200)
	@Schema(description = "Campo usado como watermark para coleta incremental")
	private String watermarkField;

	@OneToMany(mappedBy = "sourceModel", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("order ASC")
	@Schema(description = "Campos declarados do modelo da fonte")
	private List<SourceModelField> fields = new ArrayList<>();

	@Column(nullable = false)
	@Schema(description = "Indica se o modelo esta ativo")
	private Boolean active = Boolean.TRUE;

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
		if (this.itemsJsonPath == null || this.itemsJsonPath.isBlank()) {
			this.itemsJsonPath = "items";
		}
		if (this.hasNextJsonPath == null || this.hasNextJsonPath.isBlank()) {
			this.hasNextJsonPath = "hasNext";
		}
		if (this.pageParamName == null || this.pageParamName.isBlank()) {
			this.pageParamName = "page";
		}
		if (this.pageSizeParamName == null || this.pageSizeParamName.isBlank()) {
			this.pageSizeParamName = "pageSize";
		}
		if (this.pageSize == null || this.pageSize <= 0) {
			this.pageSize = 100;
		}
		if (this.sourceMethod == null) {
			this.sourceMethod = SourceMethod.GET;
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

	public List<SourceModelField> getFields() {
		return fields;
	}

	public void setFields(List<SourceModelField> fields) {
		this.fields = fields;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
