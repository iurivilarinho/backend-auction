package com.br.auction.integration.source;

import com.br.auction.integration.enums.ConnectorType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados para criacao ou atualizacao de uma fonte de integracao")
public class IntegrationSourceRequest {

	@NotBlank(message = "O codigo e obrigatorio")
	@Schema(description = "Codigo unico da fonte")
	private String code;

	@NotBlank(message = "O nome e obrigatorio")
	@Schema(description = "Nome amigavel da fonte")
	private String name;

	@Schema(description = "Descricao da fonte")
	private String description;

	@NotNull(message = "O tipo de conector e obrigatorio")
	@Schema(description = "Tipo de conector da fonte")
	private ConnectorType connectorType;

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

	@Schema(description = "Identificador da credencial usada para autenticar na fonte")
	private Long credentialId;

	@Schema(description = "Indica se a fonte esta ativa")
	private Boolean active;

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

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getJdbcUrl() {
		return jdbcUrl;
	}

	public void setJdbcUrl(String jdbcUrl) {
		this.jdbcUrl = jdbcUrl;
	}

	public String getJdbcDriver() {
		return jdbcDriver;
	}

	public void setJdbcDriver(String jdbcDriver) {
		this.jdbcDriver = jdbcDriver;
	}

	public String getProviderCode() {
		return providerCode;
	}

	public void setProviderCode(String providerCode) {
		this.providerCode = providerCode;
	}

	public String getProviderName() {
		return providerName;
	}

	public void setProviderName(String providerName) {
		this.providerName = providerName;
	}

	public String getStateCode() {
		return stateCode;
	}

	public void setStateCode(String stateCode) {
		this.stateCode = stateCode;
	}

	public String getStateName() {
		return stateName;
	}

	public void setStateName(String stateName) {
		this.stateName = stateName;
	}

	public Long getCredentialId() {
		return credentialId;
	}

	public void setCredentialId(Long credentialId) {
		this.credentialId = credentialId;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}
}
