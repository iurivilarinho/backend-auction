package com.br.auction.integration.credential;

import java.time.LocalDateTime;

import com.br.auction.integration.crypto.EncryptedStringConverter;
import com.br.auction.integration.enums.CredentialType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbIntegrationCredential")
@Schema(description = "Credencial usada para autenticar o acesso a uma fonte externa. Segredos sao armazenados criptografados.")
public class Credential {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Schema(description = "Identificador interno da credencial")
	private Long id;

	@Column(nullable = false, unique = true, length = 80)
	@Schema(description = "Codigo unico da credencial")
	private String code;

	@Column(nullable = false, length = 200)
	@Schema(description = "Nome amigavel da credencial")
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	@Schema(description = "Tipo de autenticacao")
	private CredentialType type;

	@Convert(converter = EncryptedStringConverter.class)
	@Column(length = 600)
	@Schema(description = "Usuario (criptografado)")
	private String username;

	@Convert(converter = EncryptedStringConverter.class)
	@Column(length = 1000)
	@Schema(description = "Senha (criptografada)")
	private String password;

	@Convert(converter = EncryptedStringConverter.class)
	@Column(length = 2000)
	@Schema(description = "Token estatico (criptografado)")
	private String token;

	@Column(length = 120)
	@Schema(description = "Nome do header para autenticacao por API Key")
	private String apiKeyHeader;

	@Convert(converter = EncryptedStringConverter.class)
	@Column(length = 2000)
	@Schema(description = "Valor da API Key (criptografado)")
	private String apiKeyValue;

	@Column(nullable = false)
	@Schema(description = "Indica se a credencial esta ativa")
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

	public CredentialType getType() {
		return type;
	}

	public void setType(CredentialType type) {
		this.type = type;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getApiKeyHeader() {
		return apiKeyHeader;
	}

	public void setApiKeyHeader(String apiKeyHeader) {
		this.apiKeyHeader = apiKeyHeader;
	}

	public String getApiKeyValue() {
		return apiKeyValue;
	}

	public void setApiKeyValue(String apiKeyValue) {
		this.apiKeyValue = apiKeyValue;
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
