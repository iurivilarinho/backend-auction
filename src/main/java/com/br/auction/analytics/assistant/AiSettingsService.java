package com.br.auction.analytics.assistant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuracao de conexao com a IA. Ha uma unica configuracao <b>global</b>
 * (singleton, {@code tbAiSettings}) — o app de leiloes nao tem usuarios. O
 * assistente usa essa configuracao; defaults de primeira execucao vem das
 * properties ({@code bi.assistant.*}/{@code spring.ai.ollama.*}).
 */
@Service
public class AiSettingsService {

    private final AiSettingsRepository repository;
    private final String defaultProvider;
    private final String defaultClaudeCommand;
    private final String defaultOllamaBaseUrl;
    private final String defaultOllamaModel;

    public AiSettingsService(AiSettingsRepository repository,
            @Value("${bi.assistant.provider:claude}") String defaultProvider,
            @Value("${bi.assistant.claude.command:claude}") String defaultClaudeCommand,
            @Value("${spring.ai.ollama.base-url:}") String defaultOllamaBaseUrl,
            @Value("${spring.ai.ollama.chat.options.model:}") String defaultOllamaModel) {
        this.repository = repository;
        this.defaultProvider = defaultProvider;
        this.defaultClaudeCommand = defaultClaudeCommand;
        this.defaultOllamaBaseUrl = defaultOllamaBaseUrl;
        this.defaultOllamaModel = defaultOllamaModel;
    }

    /** Configuracao global persistida, ou {@code null} quando ainda nao configurada. */
    @Transactional(readOnly = true)
    public AiSettings findGlobalOrNull() {
        return repository.findById(AiSettings.SINGLETON_ID).orElse(null);
    }

    /** Configuracao global efetiva: a salva ou, na ausencia, os defaults. */
    @Transactional(readOnly = true)
    public AiSettings global() {
        AiSettings saved = findGlobalOrNull();
        return saved != null ? saved : defaults();
    }

    /** Alias semantico usado pelo roteador/serviços: a configuracao efetiva ativa. */
    @Transactional(readOnly = true)
    public AiSettings effective() {
        return global();
    }

    @Transactional
    public AiSettings saveGlobal(AiSettingsForm form) {
        AiSettings settings = repository.findById(AiSettings.SINGLETON_ID).orElseGet(AiSettings::new);
        settings.setId(AiSettings.SINGLETON_ID);
        settings.setProvider(form.provider());
        settings.setBaseUrl(trimToNull(form.baseUrl()));
        settings.setModel(trimToNull(form.model()));
        settings.setClaudeCommand(trimToNull(form.claudeCommand()));
        settings.setTimeoutSeconds(form.timeoutSeconds() > 0 ? form.timeoutSeconds() : 120);
        settings.setBypassGuard(form.bypassGuard());
        if (form.apiKey() != null && !form.apiKey().isBlank()) {
            settings.setApiKey(form.apiKey().trim());
        }
        return repository.save(settings);
    }

    /**
     * Configuracao efetiva para o teste de conexao: combina o formulario com o token
     * ja salvo quando o campo do token vem em branco (a tela nunca recebe o token).
     */
    @Transactional(readOnly = true)
    public AiSettings mergeForTest(AiSettingsForm form) {
        AiSettings effective = new AiSettings();
        effective.setProvider(form.provider());
        effective.setBaseUrl(trimToNull(form.baseUrl()));
        effective.setModel(trimToNull(form.model()));
        effective.setClaudeCommand(trimToNull(form.claudeCommand()));
        effective.setTimeoutSeconds(form.timeoutSeconds() > 0 ? form.timeoutSeconds() : 120);
        if (form.apiKey() != null && !form.apiKey().isBlank()) {
            effective.setApiKey(form.apiKey().trim());
        } else {
            AiSettings saved = findGlobalOrNull();
            if (saved != null) {
                effective.setApiKey(saved.getApiKey());
            }
        }
        return effective;
    }

    private AiSettings defaults() {
        AiSettings settings = new AiSettings();
        settings.setProvider(parseProvider(defaultProvider));
        settings.setClaudeCommand(trimToNull(defaultClaudeCommand));
        settings.setBaseUrl(trimToNull(defaultOllamaBaseUrl));
        settings.setModel(trimToNull(defaultOllamaModel));
        settings.setTimeoutSeconds(120);
        return settings;
    }

    private AiProvider parseProvider(String raw) {
        if (raw == null) {
            return AiProvider.CLAUDE_CLI;
        }
        return switch (raw.trim().toLowerCase()) {
            case "ollama" -> AiProvider.OLLAMA;
            case "anthropic", "anthropic_api" -> AiProvider.ANTHROPIC_API;
            case "openai" -> AiProvider.OPENAI;
            case "gemini", "google" -> AiProvider.GEMINI;
            case "custom" -> AiProvider.CUSTOM;
            default -> AiProvider.CLAUDE_CLI;
        };
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Payload da configuracao global (inclui bypass). */
    public record AiSettingsForm(AiProvider provider, String apiKey, String baseUrl, String model,
            String claudeCommand, int timeoutSeconds, boolean bypassGuard) {
    }
}
