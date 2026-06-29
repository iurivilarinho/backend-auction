package com.br.auction.garage.acquisition;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resultado da sincronização de documentos do veículo arrematado a partir do painel do provedor.
 * Substitui o antigo record {@code SyncResult} que estava aninhado em {@code DetranPanelService}.
 */
@Schema(description = "Resultado da sincronização de documentos do painel do provedor")
public record DocumentSyncResultResponse(
		@Schema(description = "Indica se a sincronização foi bem-sucedida") boolean success,
		@Schema(description = "Mensagem amigável sobre o resultado") String message,
		@Schema(description = "Quantidade de documentos adicionados") int documentsAdded) {
}
