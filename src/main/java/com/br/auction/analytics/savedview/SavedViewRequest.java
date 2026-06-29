package com.br.auction.analytics.savedview;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Dados para criar/atualizar uma visão salva do B.I. Substitui o antigo record aninhado em
 * {@code BiSavedViewDtos}.
 */
@Schema(description = "Dados para salvar/atualizar uma visão do B.I.")
public record SavedViewRequest(
		@NotBlank @Schema(description = "Nome da visão") String name,
		@Schema(description = "Escopo/contexto da visão") String scope,
		@Schema(description = "Conteúdo serializado da visão (consulta, gráfico, etc.)") String payload,
		@Schema(description = "Indica se a visão é compartilhada") Boolean shared,
		@Schema(description = "Indica se a visão é favorita") Boolean favorite) {
}
