package com.br.auction.analytics.assistant;

/**
 * Abstracao do provedor de IA usado pelo assistente do B.I. Permite trocar o
 * backend de LLM (Claude CLI local, Ollama, API externa, etc.) por configuracao,
 * sem que o servico do assistente conheca a implementacao.
 */
public interface AiCompletionPort {

    /**
     * Executa uma completude de texto deterministica (temperatura 0).
     *
     * @param systemPrompt instrucoes de sistema (papel/contrato de saida).
     * @param userPrompt   conteudo do usuario.
     * @return o texto gerado pelo modelo.
     * @throws AiUnavailableException quando o provedor esta indisponivel ou falha.
     */
    String complete(String systemPrompt, String userPrompt);

    /** Identificacao do provedor ativo (para diagnostico/health). */
    String providerName();

    /** Sonda rapida: o provedor de IA esta acessivel agora? (sem custo de inferencia). */
    boolean isAvailable();
}
