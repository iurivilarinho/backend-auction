package com.br.auction.analytics.savedview;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Contratos de leitura/escrita das visoes salvas do B.I. */
public final class BiSavedViewDtos {

    private BiSavedViewDtos() {
    }

    /** Criacao/atualizacao de uma visao salva. */
    @Schema(name = "BiSavedViewRequest", description = "Dados para salvar/atualizar uma visao do B.I.")
    public record SavedViewRequest(
            @NotBlank String name,
            String scope,
            String payload,
            Boolean shared,
            Boolean favorite) {
    }

    /** Leitura de uma visao salva. */
    @Schema(name = "BiSavedViewResponse", description = "Visao salva do B.I. (leitura).")
    public record SavedViewResponse(
            Long id,
            Long ownerId,
            String name,
            String scope,
            String payload,
            boolean isDefault,
            boolean shared,
            boolean favorite,
            boolean archived,
            boolean mine,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {

        public static SavedViewResponse of(BiSavedView v) {
            return new SavedViewResponse(v.getId(), v.getOwnerId(), v.getName(), v.getScope(), v.getPayload(),
                    v.isDefault(), v.isShared(), v.isFavorite(), v.isArchived(), true, v.getCreatedAt(),
                    v.getUpdatedAt());
        }
    }
}
