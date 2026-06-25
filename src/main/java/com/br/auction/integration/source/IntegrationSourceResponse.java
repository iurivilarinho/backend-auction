package com.br.auction.integration.source;

import java.time.LocalDateTime;

import com.br.auction.integration.credential.Credential;
import com.br.auction.integration.enums.ConnectorType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representacao publica de uma fonte de integracao")
public class IntegrationSourceResponse {

	@Schema(description = "Identificador interno")
	private Long id;

	@Schema(description = "Codigo unico")
	private String code;

	@Schema(description = "Nome amigavel")
	private String name;

	@Schema(description = "Descricao da fonte")
	private String description;

	@Schema(description = "Tipo de conector da fonte")
	private ConnectorType connectorType;

	@Schema(description = "Descricao do tipo de conector")
	private String connectorTypeLabel;

	@Schema(description = "URL base para fontes REST")
	private String baseUrl;

	@Schema(description = "URL JDBC para fontes de banco de dados")
	private String jdbcUrl;

	@Schema(description = "Driver JDBC para fontes de banco de dados")
	private String jdbcDriver;

	@Schema(description = "Codigo do provedor representado por esta fonte")
	private String providerCode;

	@Schema(description = "Nome do provedor representado por esta fonte")
	private String providerName;

	@Schema(description = "Codigo do estado do provedor")
	private String stateCode;

	@Schema(description = "Nome do estado do provedor")
	private String stateName;

	@Schema(description = "Identificador da credencial vinculada")
	private Long credentialId;

	@Schema(description = "Codigo da credencial vinculada")
	private String credentialCode;

	@Schema(description = "Indica se a fonte esta ativa")
	private Boolean active;

	@Schema(description = "Data de criacao")
	private LocalDateTime createdAt;

	@Schema(description = "Data da ultima atualizacao")
	private LocalDateTime updatedAt;

	public IntegrationSourceResponse(IntegrationSource source) {
		this.id = source.getId();
		this.code = source.getCode();
		this.name = source.getName();
		this.description = source.getDescription();
		this.connectorType = source.getConnectorType();
		this.connectorTypeLabel = source.getConnectorType() == null ? null : source.getConnectorType().getLabel();
		this.baseUrl = source.getBaseUrl();
		this.jdbcUrl = source.getJdbcUrl();
		this.jdbcDriver = source.getJdbcDriver();
		this.providerCode = source.getProviderCode();
		this.providerName = source.getProviderName();
		this.stateCode = source.getStateCode();
		this.stateName = source.getStateName();
		Credential credential = source.getCredential();
		this.credentialId = credential == null ? null : credential.getId();
		this.credentialCode = credential == null ? null : credential.getCode();
		this.active = source.getActive();
		this.createdAt = source.getCreatedAt();
		this.updatedAt = source.getUpdatedAt();
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

	public String getBaseUrl() {
		return baseUrl;
	}

	public String getJdbcUrl() {
		return jdbcUrl;
	}

	public String getJdbcDriver() {
		return jdbcDriver;
	}

	public String getProviderCode() {
		return providerCode;
	}

	public String getProviderName() {
		return providerName;
	}

	public String getStateCode() {
		return stateCode;
	}

	public String getStateName() {
		return stateName;
	}

	public Long getCredentialId() {
		return credentialId;
	}

	public String getCredentialCode() {
		return credentialCode;
	}

	public Boolean getActive() {
		return active;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
