package com.br.auction.analytics.assistant;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Implementacao de {@link AiCompletionPort} usada pelo assistente do B.I. que
 * resolve o provedor de IA em runtime: a cada chamada le a configuracao salva
 * ({@link AiSettingsService}) e delega para o {@link AiProviderClient} do provedor
 * ativo. Trocar de provedor/credencial na tela passa a valer na proxima pergunta,
 * sem reiniciar a aplicacao.
 */
@Component
public class AiCompletionRouter implements AiCompletionPort {

    private final AiSettingsService settingsService;
    private final Map<AiProvider, AiProviderClient> clients = new EnumMap<>(AiProvider.class);

    public AiCompletionRouter(AiSettingsService settingsService, List<AiProviderClient> providerClients) {
        this.settingsService = settingsService;
        for (AiProviderClient client : providerClients) {
            clients.put(client.provider(), client);
        }
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        AiSettings settings = settingsService.effective();
        return client(settings.getProvider()).complete(settings, systemPrompt, userPrompt);
    }

    /** Executa uma completude com uma configuracao especifica (usado no teste de conexao). */
    public String completeWith(AiSettings settings, String systemPrompt, String userPrompt) {
        return client(settings.getProvider()).complete(settings, systemPrompt, userPrompt);
    }

    @Override
    public String providerName() {
        return settingsService.effective().getProvider().name().toLowerCase();
    }

    @Override
    public boolean isAvailable() {
        AiSettings settings = settingsService.effective();
        return client(settings.getProvider()).isAvailable(settings);
    }

    /** Sonda a disponibilidade de uma configuracao especifica (teste de conexao). */
    public boolean isAvailableWith(AiSettings settings) {
        return client(settings.getProvider()).isAvailable(settings);
    }

    private AiProviderClient client(AiProvider provider) {
        AiProviderClient client = clients.get(provider);
        if (client == null) {
            throw new AiUnavailableException("Provedor de IA nao suportado: " + provider, null);
        }
        return client;
    }
}
