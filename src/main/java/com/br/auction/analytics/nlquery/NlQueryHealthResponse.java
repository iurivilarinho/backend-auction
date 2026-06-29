package com.br.auction.analytics.nlquery;

import io.swagger.v3.oas.annotations.media.Schema;

/** Disponibilidade do provedor de IA (para a tela escolher o modo). */
@Schema(description = "Disponibilidade do provedor de IA do assistente de B.I.")
public record NlQueryHealthResponse(
		@Schema(description = "Indica se o provedor de IA está acessível agora") boolean available) {
}
