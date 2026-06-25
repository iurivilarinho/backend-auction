package com.br.auction.analytics.assistant;

/**
 * Sinaliza que o provedor de IA esta indisponivel ou falhou ao responder. O
 * assistente captura esta excecao para cair em respostas de reserva sem quebrar a
 * requisicao.
 */
public class AiUnavailableException extends RuntimeException {

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
