package com.br.auction.analytics.assistant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resultado do teste de conexão com o provedor de IA. Substitui o antigo record {@code TestResult}
 * aninhado em {@code AiSettingsDtos}.
 */
@Schema(description = "Resultado do teste de conexão com o provedor de IA")
public record AiSettingsTestResultResponse(
		@Schema(description = "Indica se o provedor está disponível") boolean available,
		@Schema(description = "Mensagem amigável sobre o resultado do teste") String message,
		@Schema(description = "Amostra da resposta do provedor (quando disponível)") String sample) {
}
