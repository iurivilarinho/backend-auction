package com.br.auction.integration.bootstrap;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.br.auction.integration.enums.RunStatus;
import com.br.auction.integration.execution.IntegrationRun;
import com.br.auction.integration.execution.IntegrationRunRepository;

/**
 * Marca como FALHA as execucoes que ficaram presas em RUNNING quando a aplicacao foi reiniciada
 * ou caiu no meio de uma coleta. Nenhuma execucao sobrevive a um reinicio da JVM, entao qualquer
 * run ainda RUNNING no boot e orfa e bloquearia novos disparos da mesma integracao (o scheduler
 * ignora integracoes que ja estao "em execucao").
 */
@Component
@Order(5)
public class OrphanRunReconciler implements CommandLineRunner {

	private static final Logger LOG = LoggerFactory.getLogger(OrphanRunReconciler.class);

	private final IntegrationRunRepository runRepository;

	public OrphanRunReconciler(IntegrationRunRepository runRepository) {
		this.runRepository = runRepository;
	}

	@Override
	@Transactional
	public void run(String... args) {
		List<IntegrationRun> orphans = runRepository.findByStatusOrderByStartedAtDesc(RunStatus.RUNNING);
		if (orphans.isEmpty()) {
			return;
		}
		LocalDateTime now = LocalDateTime.now();
		for (IntegrationRun run : orphans) {
			run.setStatus(RunStatus.FAILED);
			run.setErrorMessage("Execucao interrompida por reinicio da aplicacao.");
			run.setFinishedAt(now);
			if (run.getStartedAt() != null) {
				run.setDurationMs(java.time.Duration.between(run.getStartedAt(), now).toMillis());
			}
		}
		runRepository.saveAll(orphans);
		LOG.info("Reconciliacao de execucoes orfas: {} execucao(oes) presa(s) em RUNNING marcada(s) como FALHA.",
				orphans.size());
	}
}
