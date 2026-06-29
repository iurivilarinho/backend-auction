package com.br.auction.analytics.savedview;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Visão salva do B.I. (leitura). Substitui o antigo record aninhado em {@code BiSavedViewDtos}.
 */
@Schema(description = "Visão salva do B.I. (leitura)")
public record SavedViewResponse(
		@Schema(description = "ID da visão") Long id,
		@Schema(description = "ID do dono da visão") Long ownerId,
		@Schema(description = "Nome da visão") String name,
		@Schema(description = "Escopo/contexto da visão") String scope,
		@Schema(description = "Conteúdo serializado da visão") String payload,
		@Schema(description = "Indica se é a visão padrão") boolean isDefault,
		@Schema(description = "Indica se é compartilhada") boolean shared,
		@Schema(description = "Indica se é favorita") boolean favorite,
		@Schema(description = "Indica se está arquivada") boolean archived,
		@Schema(description = "Indica se a visão é do próprio usuário") boolean mine,
		@Schema(description = "Data/hora de criação") LocalDateTime createdAt,
		@Schema(description = "Data/hora da última atualização") LocalDateTime updatedAt) {

	public SavedViewResponse(BiSavedView view) {
		this(view.getId(), view.getOwnerId(), view.getName(), view.getScope(), view.getPayload(),
				view.isDefault(), view.isShared(), view.isFavorite(), view.isArchived(), true,
				view.getCreatedAt(), view.getUpdatedAt());
	}
}
