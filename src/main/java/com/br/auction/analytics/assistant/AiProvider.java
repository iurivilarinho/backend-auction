package com.br.auction.analytics.assistant;

/**
 * Formas de conexao com a IA suportadas pelo assistente do B.I. Cada valor e um
 * provedor concreto (uma implementacao de {@link AiProviderClient}); o provedor
 * ativo e escolhido em runtime pela configuracao salva em {@code tbAiSettings}.
 */
public enum AiProvider {

    /** Claude via CLI local headless ({@code claude -p}) — nao usa token. */
    CLAUDE_CLI("Claude CLI (local)", false),

    /** Claude via API da Anthropic (token {@code x-api-key}). */
    ANTHROPIC_API("Anthropic API (Claude)", true),

    /** GPT via API da OpenAI (token {@code Bearer}); aceita endpoints compativeis. */
    OPENAI("OpenAI (GPT)", true),

    /** Gemini via API do Google AI Studio (token na query {@code key}). */
    GEMINI("Google Gemini", true),

    /** Ollama local/auto-hospedado (sem token; URL + modelo). */
    OLLAMA("Ollama (local)", false),

    /**
     * Modelo proprio / endpoint generico (ex.: gpt-cli local) — aceita qualquer
     * coisa: faz POST {baseUrl}/chat com {"message":...} e le a resposta de forma
     * tolerante (reply/response/text/content...). Sem token; so a URL base.
     */
    CUSTOM("Modelo proprio", false);

    private final String label;
    private final boolean requiresApiKey;

    AiProvider(String label, boolean requiresApiKey) {
        this.label = label;
        this.requiresApiKey = requiresApiKey;
    }

    public String getLabel() {
        return label;
    }

    /** Indica se o provedor exige uma chave/token de API para funcionar. */
    public boolean isRequiresApiKey() {
        return requiresApiKey;
    }
}
