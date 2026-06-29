package com.br.auction.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Lote no formato do feed do provedor (mesmo contrato JSON consumido pelo módulo de integração).
 * Substitui o antigo {@code Map<String, Object>} por um modelo tipado e documentado. Os valores
 * monetários são expostos como texto porque a fonte é declarada com campos {@code STRING} e o sink
 * faz a conversão lenient (inclusive formato {@code MONEY_BR}). Campos nulos são omitidos para
 * preservar o conjunto de chaves de cada provedor.
 */
@Schema(description = "Lote do feed do provedor")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LotFeedResponse {

	@Schema(description = "Identificador do leilão pai na fonte")
	private final String auctionId;

	@Schema(description = "Identificador do lote na fonte")
	private final String lotId;

	@Schema(description = "Número/rótulo do lote")
	private final String lotNumber;

	@Schema(description = "Tipo/condição do lote no padrão do sistema")
	private final String lotType;

	@Schema(description = "Descrição do veículo")
	private final String vehicleDescription;

	@Schema(description = "Ano do veículo")
	private final String vehicleYear;

	@Schema(description = "Valor do lance atual (texto)")
	private final String currentBidValue;

	@Schema(description = "Valor mínimo/piso do lance (texto)")
	private final String minimumBidValue;

	@Schema(description = "Data/hora de encerramento do lote (formato da fonte)")
	private final String closingDate;

	@Schema(description = "Situação do lote na fonte")
	private final String lotStatus;

	@Schema(description = "URLs das imagens do lote")
	private final List<String> imageUrls;

	public LotFeedResponse(String auctionId, String lotId, String lotNumber, String lotType, String vehicleDescription,
			String vehicleYear, String currentBidValue, String minimumBidValue, String closingDate, String lotStatus,
			List<String> imageUrls) {
		this.auctionId = auctionId;
		this.lotId = lotId;
		this.lotNumber = lotNumber;
		this.lotType = lotType;
		this.vehicleDescription = vehicleDescription;
		this.vehicleYear = vehicleYear;
		this.currentBidValue = currentBidValue;
		this.minimumBidValue = minimumBidValue;
		this.closingDate = closingDate;
		this.lotStatus = lotStatus;
		this.imageUrls = imageUrls;
	}

	/** Mapeia um lote do DETRAN (já convertido em {@link LotResponse}) para o formato do feed. */
	public LotFeedResponse(LotResponse lot, String auctionId) {
		this(auctionId, lot.getLotId(), lot.getLotNumber(), lot.getLotType(), lot.getVehicleDescription(), null,
				lot.getCurrentBidValue(), null, null, null, lot.getImageUrls());
	}

	public String getAuctionId() {
		return auctionId;
	}

	public String getLotId() {
		return lotId;
	}

	public String getLotNumber() {
		return lotNumber;
	}

	public String getLotType() {
		return lotType;
	}

	public String getVehicleDescription() {
		return vehicleDescription;
	}

	public String getVehicleYear() {
		return vehicleYear;
	}

	public String getCurrentBidValue() {
		return currentBidValue;
	}

	public String getMinimumBidValue() {
		return minimumBidValue;
	}

	public String getClosingDate() {
		return closingDate;
	}

	public String getLotStatus() {
		return lotStatus;
	}

	public List<String> getImageUrls() {
		return imageUrls;
	}
}
