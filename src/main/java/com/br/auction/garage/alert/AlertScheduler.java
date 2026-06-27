package com.br.auction.garage.alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Agendador que dispara a avaliacao dos alertas em intervalos regulares. Espelha o padrao do
 * {@code IntegrationScheduler}: intervalo e atraso inicial configuraveis, com flag de liga/desliga.
 */
@Component
public class AlertScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(AlertScheduler.class);

	private final AlertEvaluator evaluator;
	private final boolean enabled;

	public AlertScheduler(AlertEvaluator evaluator, @Value("${whatsapp.alert.enabled:true}") boolean enabled) {
		this.evaluator = evaluator;
		this.enabled = enabled;
	}

	@Scheduled(fixedDelayString = "${whatsapp.alert.interval-ms:300000}", initialDelayString = "${whatsapp.alert.initial-delay-ms:60000}")
	public void runScheduled() {
		if (!enabled) {
			return;
		}
		try {
			evaluator.evaluateAll();
		} catch (RuntimeException ex) {
			LOG.warn("Falha na avaliacao agendada de alertas: {}", ex.getMessage());
		}
	}

	/** Disparo manual (usado pela tela/endpoint para testar sem esperar o intervalo). */
	public void triggerNow() {
		evaluator.evaluateAll();
	}
}
