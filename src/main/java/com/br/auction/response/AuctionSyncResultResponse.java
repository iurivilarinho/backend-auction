package com.br.auction.response;

import java.time.LocalDateTime;

import com.br.auction.enums.AuctionProvider;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado da sincronizacao de leiloes com o provedor")
public class AuctionSyncResultResponse {

	@Schema(description = "Codigo do provedor")
	private String providerCode;

	@Schema(description = "Nome do provedor")
	private String providerName;

	@Schema(description = "Codigo do estado")
	private String stateCode;

	@Schema(description = "Nome do estado")
	private String stateName;

	@Schema(description = "Total de leiloes encontrados na fonte")
	private Integer totalSourceAuctions;

	@Schema(description = "Quantidade de leiloes importados")
	private Integer importedAuctions;

	@Schema(description = "Quantidade de leiloes atualizados")
	private Integer updatedAuctions;

	@Schema(description = "Quantidade de leiloes ignorados")
	private Integer skippedAuctions;

	@Schema(description = "Quantidade de itens importados")
	private Integer importedItems;

	@Schema(description = "Quantidade de itens ignorados")
	private Integer skippedItems;

	@Schema(description = "Inicio da sincronizacao")
	private LocalDateTime startedAt;

	@Schema(description = "Fim da sincronizacao")
	private LocalDateTime finishedAt;

	public AuctionSyncResultResponse(AuctionProvider provider, Integer totalSourceAuctions, Integer importedAuctions,
			Integer updatedAuctions, Integer skippedAuctions, Integer importedItems, Integer skippedItems,
			LocalDateTime startedAt, LocalDateTime finishedAt) {
		this.providerCode = provider.getCode();
		this.providerName = provider.getName();
		this.stateCode = provider.getStateCode();
		this.stateName = provider.getStateName();
		this.totalSourceAuctions = totalSourceAuctions;
		this.importedAuctions = importedAuctions;
		this.updatedAuctions = updatedAuctions;
		this.skippedAuctions = skippedAuctions;
		this.importedItems = importedItems;
		this.skippedItems = skippedItems;
		this.startedAt = startedAt;
		this.finishedAt = finishedAt;
	}

	public String getProviderCode() {
		return providerCode;
	}

	public String getProviderName() {
		return providerName;
	}

	public String getStateCode() {
		return stateCode;
	}

	public String getStateName() {
		return stateName;
	}

	public Integer getTotalSourceAuctions() {
		return totalSourceAuctions;
	}

	public Integer getImportedAuctions() {
		return importedAuctions;
	}

	public Integer getUpdatedAuctions() {
		return updatedAuctions;
	}

	public Integer getSkippedAuctions() {
		return skippedAuctions;
	}

	public Integer getImportedItems() {
		return importedItems;
	}

	public Integer getSkippedItems() {
		return skippedItems;
	}

	public LocalDateTime getStartedAt() {
		return startedAt;
	}

	public LocalDateTime getFinishedAt() {
		return finishedAt;
	}
}
