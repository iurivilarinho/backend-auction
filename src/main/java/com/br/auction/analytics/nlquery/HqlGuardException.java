package com.br.auction.analytics.nlquery;

/** Falha de validacao do HQL gerado pela IA (escrita, multiplos comandos, entidade fora da whitelist). */
public class HqlGuardException extends RuntimeException {

    public HqlGuardException(String message) {
        super(message);
    }
}
