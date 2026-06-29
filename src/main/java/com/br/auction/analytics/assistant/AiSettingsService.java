package com.br.auction.analytics.assistant;

import java.util.Arrays;
import java.util.List;

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

    /** Configuração atual efetiva, no formato de resposta (sem expor o token). */
    @Transactional(readOnly = true)
    public AiSettingsResponse currentSettings() {
        return new AiSettingsResponse(global());
    }

    /** Provedores de IA disponíveis para seleção na tela. */
    public List<AiProviderOptionResponse> providerOptions() {
        return Arrays.stream(AiProvider.values())
                .map(provider -> new AiProviderOptionResponse(provider.name(), provider.getLabel(),
                        provider.isRequiresApiKey()))
                .toList();
    }

    @Transactional
    public AiSettings saveGlobal(AiSettingsRequest request) {
        AiSettings settings = repository.findById(AiSettings.SINGLETON_ID).orElseGet(AiSettings::new);
        settings.setId(AiSettings.SINGLETON_ID);
        settings.setProvider(request.provider() != null ? request.provider() : AiProvider.CLAUDE_CLI);
        settings.setBaseUrl(trimToNull(request.baseUrl()));
        settings.setModel(trimToNull(request.model()));
        settings.setClaudeCommand(trimToNull(request.claudeCommand()));
        settings.setTimeoutSeconds(resolveTimeout(request.timeoutSeconds()));
        settings.setBypassGuard(Boolean.TRUE.equals(request.bypassGuard()));
        if (request.apiKey() != null && !request.apiKey().isBlank()) {
            settings.setApiKey(request.apiKey().trim());
        }
        return repository.save(settings);
    }

    /**
     * Configuracao efetiva para o teste de conexao: combina o formulario com o token
     * ja salvo quando o campo do token vem em branco (a tela nunca recebe o token).
     */
    @Transactional(readOnly = true)
    public AiSettings mergeForTest(AiSettingsRequest request) {
        AiSettings effective = new AiSettings();
        effective.setProvider(request.provider() != null ? request.provider() : AiProvider.CLAUDE_CLI);
        effective.setBaseUrl(trimToNull(request.baseUrl()));
        effective.setModel(trimToNull(request.model()));
        effective.setClaudeCommand(trimToNull(request.claudeCommand()));
        effective.setTimeoutSeconds(resolveTimeout(request.timeoutSeconds()));
        if (request.apiKey() != null && !request.apiKey().isBlank()) {
            effective.setApiKey(request.apiKey().trim());
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

    private int resolveTimeout(Integer timeoutSeconds) {
        return timeoutSeconds != null && timeoutSeconds > 0 ? timeoutSeconds : 120;
    }
}
