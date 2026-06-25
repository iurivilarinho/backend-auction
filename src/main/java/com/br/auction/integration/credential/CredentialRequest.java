package com.br.auction.integration.credential;

import com.br.auction.integration.enums.CredentialType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados para criacao ou atualizacao de uma credencial")
public class CredentialRequest {

	@NotBlank(message = "O codigo e obrigatorio")
	@Schema(description = "Codigo unico da credencial")
	private String code;

	@NotBlank(message = "O nome e obrigatorio")
	@Schema(description = "Nome amigavel da credencial")
	private String name;

	@NotNull(message = "O tipo e obrigatorio")
	@Schema(description = "Tipo de autenticacao")
	private CredentialType type;

	@Schema(description = "Usuario")
	private String username;

	@Schema(description = "Senha")
	private String password;

	@Schema(description = "Token estatico")
	private String token;

	@Schema(description = "Nome do header da API Key")
	private String apiKeyHeader;

	@Schema(description = "Valor da API Key")
	private String apiKeyValue;

	@Schema(description = "Indica se a credencial esta ativa")
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
}
