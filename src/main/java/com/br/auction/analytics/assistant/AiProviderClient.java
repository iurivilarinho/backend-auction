package com.br.auction.analytics.assistant;

/**
 * Cliente de um provedor de IA concreto (Claude CLI, Anthropic API, OpenAI,
 * Gemini, Ollama, modelo proprio). Recebe a configuracao resolvida em cada
 * chamada — assim a troca de provedor/credencial vale em runtime, sem reiniciar a
 * aplicacao. O {@link AiCompletionRouter} seleciona o cliente conforme o provedor
 * salvo.
 */
public interface AiProviderClient {

    /** Provedor atendido por esta implementacao. */
    AiProvider provider();

    /**
     * Executa uma completude de texto deterministica (temperatura 0).
     *
     * @param settings     configuracao ativa (token, URL, modelo, timeout).
     * @param systemPrompt instrucoes de sistema.
     * @param userPrompt   conteudo do usuario.
     * @return o texto gerado pelo modelo.
     * @throws AiUnavailableException quando o provedor esta indisponivel ou falha.
     */
    String complete(AiSettings settings, String systemPrompt, String userPrompt);

    /** Sonda rapida: o provedor esta acessivel com esta configuracao? (sem custo de inferencia). */
    boolean isAvailable(AiSettings settings);
}
