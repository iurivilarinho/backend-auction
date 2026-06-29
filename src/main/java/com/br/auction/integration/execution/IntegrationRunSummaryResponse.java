package com.br.auction.integration.execution;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resumo das execuções de integração por status (contagem). Substitui o antigo
 * {@code Map<String, Long>} por um modelo tipado; as chaves JSON são mantidas em maiúsculas (nomes
 * de {@code RunStatus}) via {@code @JsonProperty} para preservar o contrato consumido pelo frontend.
 */
@Schema(description = "Resumo das execuções de integração por status")
public class IntegrationRunSummaryResponse {

	@JsonProperty("RUNNING")
	@Schema(description = "Execuções em andamento")
	private final long running;

	@JsonProperty("SUCCESS")
	@Schema(description = "Execuções concluídas com sucesso")
	private final long success;

	@JsonProperty("PARTIAL")
	@Schema(description = "Execuções concluídas com falhas parciais")
	private final long partial;

	@JsonProperty("FAILED")
	@Schema(description = "Execuções que falharam")
	private final long failed;

	@JsonProperty("CANCELLED")
	@Schema(description = "Execuções canceladas")
	private final long cancelled;

	public IntegrationRunSummaryResponse(long running, long success, long partial, long failed, long cancelled) {
		this.running = running;
		this.success = success;
		this.partial = partial;
		this.failed = failed;
		this.cancelled = cancelled;
	}

	public long getRunning() {
		return running;
	}

	public long getSuccess() {
		return success;
	}

	public long getPartial() {
		return partial;
	}

	public long getFailed() {
		return failed;
	}

	public long getCancelled() {
		return cancelled;
	}
}
