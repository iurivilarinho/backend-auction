package com.br.auction.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Situacao da busca FIPE em segundo plano de um leilao")
public class FipeEnrichStatusResponse {

	@Schema(description = "Indica se a busca FIPE ainda esta em andamento", example = "true")
	private final boolean running;

	@Schema(description = "Total de itens a processar na rodada", example = "120")
	private final int total;

	@Schema(description = "Quantidade de itens ja processados", example = "40")
	private final int processed;

	@Schema(description = "Quantidade de itens que receberam valor FIPE", example = "35")
	private final int enriched;

	@Schema(description = "Mensagem amigavel sobre a situacao do processamento")
	private final String message;

	public FipeEnrichStatusResponse(boolean running, int total, int processed, int enriched, String message) {
		this.running = running;
		this.total = total;
		this.processed = processed;
		this.enriched = enriched;
		this.message = message;
	}

	public boolean isRunning() {
		return running;
	}

	public int getTotal() {
		return total;
	}

	public int getProcessed() {
		return processed;
	}

	public int getEnriched() {
		return enriched;
	}

	public String getMessage() {
		return message;
	}
}
