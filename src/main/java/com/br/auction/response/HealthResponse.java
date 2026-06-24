package com.br.auction.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta de saude da API")
public class HealthResponse {

	@Schema(description = "Status da API")
	private String status;

	@Schema(description = "Banco configurado para a execucao atual")
	private String database;

	@Schema(description = "Provedor padrao da execucao atual")
	private ProviderResponse defaultProvider;

	public HealthResponse(String status, String database, ProviderResponse defaultProvider) {
		this.status = status;
		this.database = database;
		this.defaultProvider = defaultProvider;
	}

	public String getStatus() {
		return status;
	}

	public String getDatabase() {
		return database;
	}

	public ProviderResponse getDefaultProvider() {
		return defaultProvider;
	}
}
