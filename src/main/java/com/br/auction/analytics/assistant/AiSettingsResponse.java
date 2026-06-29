package com.br.auction.analytics.assistant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Configuração atual da integração com IA exibida na tela (nunca expõe o token). Substitui o antigo
 * record {@code AiSettingsView} que estava aninhado em {@code AiSettingsDtos}.
 */
@Schema(description = "Configuração atual da integração com IA")
public record AiSettingsResponse(
		@Schema(description = "Provedor de IA selecionado") AiProvider provider,
		@Schema(description = "URL base do provedor (quando aplicável)") String baseUrl,
		@Schema(description = "Modelo configurado") String model,
		@Schema(description = "Comando do Claude CLI (quando aplicável)") String claudeCommand,
		@Schema(description = "Timeout em segundos") int timeoutSeconds,
		@Schema(description = "Indica se o guardrail está desativado") boolean bypassGuard,
		@Schema(description = "Indica se há um token salvo (sem expô-lo)") boolean hasApiKey) {

	public AiSettingsResponse(AiSettings settings) {
		this(settings.getProvider(), settings.getBaseUrl(), settings.getModel(), settings.getClaudeCommand(),
				settings.getTimeoutSeconds(), settings.isBypassGuard(), settings.hasApiKey());
	}
}
