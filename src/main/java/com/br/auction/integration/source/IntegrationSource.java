package com.br.auction.integration.source;

import java.time.LocalDateTime;

import com.br.auction.integration.credential.Credential;
import com.br.auction.integration.enums.ConnectorType;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Fonte/provedor de dados de uma integracao. A aplicacao atual e sempre o destino,
 * portanto so a fonte e cadastrada. Cada fonte representa um provedor (ex.: DETRAN_MG)
 * e e modelada de forma extensivel para novos estados/provedores.
 */
@Entity
@Table(name = "tbIntegrationSource")
@Schema(description = "Fonte externa (provedor) de onde os dados sao coletados para os modelos internos da aplicacao")
public class IntegrationSource {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Schema(description = "Identificador interno da fonte")
	private Long id;

	@Column(nullable = false, unique = true, length = 80)
	@Schema(description = "Codigo unico da fonte")
	private String code;

	@Column(nullable = false, length = 200)
	@Schema(description = "Nome amigavel da fonte")
	private String name;

	@Column(length = 1000)
	@Schema(description = "Descricao da fonte")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	@Schema(description = "Tipo de conector da fonte")
	private ConnectorType connectorType;

	@Column(length = 500)
	@Schema(description = "URL base para fontes REST")
	private String baseUrl;

	@Column(length = 500)
	@Schema(description = "URL JDBC para fontes de banco de dados")
	private String jdbcUrl;

	@Column(length = 200)
	@Schema(description = "Driver JDBC para fontes de banco de dados")
	private String jdbcDriver;

	@Column(length = 80)
	@Schema(description = "Codigo do provedor representado por esta fonte (ex.: DETRAN_MG)")
	private String providerCode;

	@Column(length = 200)
	@Schema(description = "Nome do provedor representado por esta fonte")
	private String providerName;

	@Column(length = 10)
	@Schema(description = "Codigo do estado do provedor")
	private String stateCode;

	@Column(length = 100)
	@Schema(description = "Nome do estado do provedor")
	private String stateName;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "fk_Id_Credential", foreignKey = @ForeignKey(name = "FK_FROM_TBINTEGRATIONSOURCE_FOR_TBINTEGRATIONCREDENTIAL"))
	@Schema(description = "Credencial usada para autenticar na fonte")
	private Credential credential;

	@Column(nullable = false)
	@Schema(description = "Indica se a fonte esta ativa")
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
		if (this.active == null) {
			this.active = Boolean.TRUE;
		}
	}

	@PreUpdate
	public void onUpdate() {
		this.updatedAt = LocalDateTime.now();
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

	public Credential getCredential() {
		return credential;
	}

	public void setCredential(Credential credential) {
		this.credential = credential;
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
