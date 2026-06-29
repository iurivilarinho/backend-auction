package com.br.auction.analytics.assistant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Dados para salvar/testar a integração com IA. O token em branco preserva o token já salvo.
 * Substitui o antigo record {@code AiSettingsRequest} aninhado em {@code AiSettingsDtos}.
 */
@Schema(description = "Dados para salvar/testar a integração com IA")
public record AiSettingsRequest(
		@Schema(description = "Provedor de IA") AiProvider provider,
		@Schema(description = "Token/credencial do provedor (em branco mantém o já salvo)") String apiKey,
		@Schema(description = "URL base do provedor (quando aplicável)") String baseUrl,
		@Schema(description = "Modelo a usar") String model,
		@Schema(description = "Comando do Claude CLI (quando aplicável)") String claudeCommand,
		@Schema(description = "Timeout em segundos (padrão 120)") Integer timeoutSeconds,
		@Schema(description = "Desativar o guardrail") Boolean bypassGuard) {
}
