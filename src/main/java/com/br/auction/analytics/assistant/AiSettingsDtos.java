package com.br.auction.analytics.assistant;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** DTOs da configuracao de conexao com a IA do B.I. */
public final class AiSettingsDtos {

    private AiSettingsDtos() {
    }

    /** Configuracao atual exibida na tela (sem expor o token). */
    @Schema(name = "AiSettingsView", description = "Configuracao atual da integracao com IA.")
    public record AiSettingsView(
            AiProvider provider,
            String baseUrl,
            String model,
            String claudeCommand,
            int timeoutSeconds,
            boolean bypassGuard,
            boolean hasApiKey) {

        public static AiSettingsView of(AiSettings s) {
            return new AiSettingsView(s.getProvider(), s.getBaseUrl(), s.getModel(), s.getClaudeCommand(),
                    s.getTimeoutSeconds(), s.isBypassGuard(), s.hasApiKey());
        }
    }

    /** Payload de salvamento/teste. O token em branco preserva o token ja salvo. */
    @Schema(name = "AiSettingsRequest", description = "Dados para salvar/testar a integracao com IA.")
    public record AiSettingsRequest(
            AiProvider provider,
            String apiKey,
            String baseUrl,
            String model,
            String claudeCommand,
            Integer timeoutSeconds,
            Boolean bypassGuard) {

        public AiSettingsService.AiSettingsForm toForm() {
            return new AiSettingsService.AiSettingsForm(
                    provider != null ? provider : AiProvider.CLAUDE_CLI,
                    apiKey,
                    baseUrl,
                    model,
                    claudeCommand,
                    timeoutSeconds != null ? timeoutSeconds : 120,
                    Boolean.TRUE.equals(bypassGuard));
        }
    }

    /** Resultado do teste de conexao com o provedor. */
    @Schema(name = "AiSettingsTestResult")
    public record TestResult(boolean available, String message, String sample) {
    }

    /** Provedor disponivel para selecao na tela. */
    @Schema(name = "AiProviderOption")
    public record ProviderOption(String value, String label, boolean requiresApiKey) {

        public static List<ProviderOption> all() {
            return List.of(AiProvider.values()).stream()
                    .map(p -> new ProviderOption(p.name(), p.getLabel(), p.isRequiresApiKey()))
                    .toList();
        }
    }
}
