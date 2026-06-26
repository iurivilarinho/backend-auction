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

    /** Janela em que consideramos a IA "no ar" apos um sucesso, sem re-sondar. */
    private static final long OK_CACHE_MS = 120_000;

    private final AiSettingsService settingsService;
    private final Map<AiProvider, AiProviderClient> clients = new EnumMap<>(AiProvider.class);

    /**
     * Momento do ultimo sucesso (completude ou sonda). Evita o "IA fora" intermitente:
     * provedores locais single-thread (ex.: gpt-cli) nao respondem o /status enquanto
     * processam uma pergunta, o que derrubava o health justo durante o uso.
     */
    private volatile long lastOkAt = 0L;

    public AiCompletionRouter(AiSettingsService settingsService, List<AiProviderClient> providerClients) {
        this.settingsService = settingsService;
        for (AiProviderClient client : providerClients) {
            clients.put(client.provider(), client);
        }
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        AiSettings settings = settingsService.effective();
        String result = client(settings.getProvider()).complete(settings, systemPrompt, userPrompt);
        lastOkAt = System.currentTimeMillis();
        return result;
    }

    /** Executa uma completude com uma configuracao especifica (usado no teste de conexao). */
    public String completeWith(AiSettings settings, String systemPrompt, String userPrompt) {
        String result = client(settings.getProvider()).complete(settings, systemPrompt, userPrompt);
        lastOkAt = System.currentTimeMillis();
        return result;
    }

    @Override
    public String providerName() {
        return settingsService.effective().getProvider().name().toLowerCase();
    }

    @Override
    public boolean isAvailable() {
        // Se uma pergunta/sonda funcionou ha pouco, nao re-sonda (o provedor pode estar
        // ocupado respondendo outra pergunta agora e falsamente parecer "fora").
        if (System.currentTimeMillis() - lastOkAt < OK_CACHE_MS) {
            return true;
        }
        AiSettings settings = settingsService.effective();
        boolean ok = client(settings.getProvider()).isAvailable(settings);
        if (ok) {
            lastOkAt = System.currentTimeMillis();
        }
        return ok;
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
