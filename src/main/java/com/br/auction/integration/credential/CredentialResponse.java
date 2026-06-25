package com.br.auction.integration.credential;

import java.time.LocalDateTime;

import com.br.auction.integration.enums.CredentialType;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resposta de credencial. Nunca expoe segredos: apenas indica se cada segredo esta
 * preenchido.
 */
@Schema(description = "Representacao publica de uma credencial, sem expor segredos")
public class CredentialResponse {

	@Schema(description = "Identificador interno")
	private Long id;

	@Schema(description = "Codigo unico")
	private String code;

	@Schema(description = "Nome amigavel")
	private String name;

	@Schema(description = "Tipo de autenticacao")
	private CredentialType type;

	@Schema(description = "Descricao do tipo de autenticacao")
	private String typeLabel;

	@Schema(description = "Nome do header da API Key")
	private String apiKeyHeader;

	@Schema(description = "Indica se ha usuario configurado")
	private boolean hasUsername;

	@Schema(description = "Indica se ha senha configurada")
	private boolean hasPassword;

	@Schema(description = "Indica se ha token configurado")
	private boolean hasToken;

	@Schema(description = "Indica se ha API Key configurada")
	private boolean hasApiKeyValue;

	@Schema(description = "Indica se a credencial esta ativa")
	private Boolean active;

	@Schema(description = "Data de criacao")
	private LocalDateTime createdAt;

	@Schema(description = "Data da ultima atualizacao")
	private LocalDateTime updatedAt;

	public CredentialResponse(Credential credential) {
		this.id = credential.getId();
		this.code = credential.getCode();
		this.name = credential.getName();
		this.type = credential.getType();
		this.typeLabel = credential.getType() == null ? null : credential.getType().getLabel();
		this.apiKeyHeader = credential.getApiKeyHeader();
		this.hasUsername = isFilled(credential.getUsername());
		this.hasPassword = isFilled(credential.getPassword());
		this.hasToken = isFilled(credential.getToken());
		this.hasApiKeyValue = isFilled(credential.getApiKeyValue());
		this.active = credential.getActive();
		this.createdAt = credential.getCreatedAt();
		this.updatedAt = credential.getUpdatedAt();
	}

	private boolean isFilled(String value) {
		return value != null && !value.isBlank();
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

	public CredentialType getType() {
		return type;
	}

	public String getTypeLabel() {
		return typeLabel;
	}

	public String getApiKeyHeader() {
		return apiKeyHeader;
	}

	public boolean isHasUsername() {
		return hasUsername;
	}

	public boolean isHasPassword() {
		return hasPassword;
	}

	public boolean isHasToken() {
		return hasToken;
	}

	public boolean isHasApiKeyValue() {
		return hasApiKeyValue;
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
