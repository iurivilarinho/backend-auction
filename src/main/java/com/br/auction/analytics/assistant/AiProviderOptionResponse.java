package com.br.auction.analytics.assistant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Provedor de IA disponível para seleção na tela. Substitui o antigo record {@code ProviderOption}
 * aninhado em {@code AiSettingsDtos}.
 */
@Schema(description = "Provedor de IA disponível para seleção")
public record AiProviderOptionResponse(
		@Schema(description = "Valor técnico (nome do provedor)") String value,
		@Schema(description = "Rótulo amigável") String label,
		@Schema(description = "Indica se o provedor exige token/credencial") boolean requiresApiKey) {
}
