package com.br.auction.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Lote AO VIVO no formato do feed (lance, prazo e status apurados lote a lote). Mantém o mesmo
 * contrato JSON consumido pelo módulo de integração, agora tipado. Campos nulos são omitidos.
 */
@Schema(description = "Lote ao vivo do feed do provedor")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LotLiveFeedResponse {

	@Schema(description = "Identificador do leilão pai")
	private final String auctionId;

	@Schema(description = "Identificador do lote na fonte")
	private final String lotId;

	@Schema(description = "Valor do lance atual (texto)")
	private final String currentBidValue;

	@Schema(description = "Data/hora de encerramento do lote (ISO)")
	private final String closingDate;

	@Schema(description = "Situação do leilão/lote na fonte")
	private final String lotStatus;

	public LotLiveFeedResponse(String auctionId, String lotId, String currentBidValue, String closingDate,
			String lotStatus) {
		this.auctionId = auctionId;
		this.lotId = lotId;
		this.currentBidValue = currentBidValue;
		this.closingDate = closingDate;
		this.lotStatus = lotStatus;
	}

	public String getAuctionId() {
		return auctionId;
	}

	public String getLotId() {
		return lotId;
	}

	public String getCurrentBidValue() {
		return currentBidValue;
	}

	public String getClosingDate() {
		return closingDate;
	}

	public String getLotStatus() {
		return lotStatus;
	}
}
