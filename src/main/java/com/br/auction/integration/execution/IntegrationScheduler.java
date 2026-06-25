package com.br.auction.integration.execution;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.br.auction.integration.enums.IntegrationStatus;
import com.br.auction.integration.enums.RunStatus;
import com.br.auction.integration.enums.TriggerMode;
import com.br.auction.integration.enums.TriggerType;
import com.br.auction.integration.integration.Integration;
import com.br.auction.integration.integration.IntegrationRepository;

/**
 * Dispara periodicamente as integracoes agendadas (TriggerMode.SCHEDULED + ACTIVE). Como
 * ninguem empurra dados do provedor para a aplicacao, a coleta e feita ativamente aqui, de
 * forma recorrente (a cada 15 minutos por padrao). Todas as informacoes do provedor passam
 * pelo modulo de integracao.
 */
@Component
public class IntegrationScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(IntegrationScheduler.class);

	private final IntegrationRepository integrationRepository;
	private final IntegrationRunRepository runRepository;
	private final IntegrationExecutor executor;
	private final boolean enabled;

	public IntegrationScheduler(IntegrationRepository integrationRepository, IntegrationRunRepository runRepository,
			IntegrationExecutor executor, @Value("${integration.scheduler.enabled:true}") boolean enabled) {
		this.integrationRepository = integrationRepository;
		this.runRepository = runRepository;
		this.executor = executor;
		this.enabled = enabled;
	}

	@Scheduled(fixedDelayString = "${integration.scheduler.interval-ms:900000}", initialDelayString = "${integration.scheduler.initial-delay-ms:20000}")
	public void runScheduled() {
		if (!enabled) {
			return;
		}
		runDueIntegrations(TriggerType.SCHEDULED);
	}

	/**
	 * Dispara agora todas as integracoes agendadas ativas. Usado pelo botao "Atualizar fonte".
	 */
	public int triggerNow() {
		return runDueIntegrations(TriggerType.MANUAL);
	}

	private int runDueIntegrations(TriggerType triggerType) {
		List<Integration> scheduled = integrationRepository.findByTriggerModeAndActiveTrue(TriggerMode.SCHEDULED);
		int started = 0;
		for (Integration integration : scheduled) {
			if (integration.getStatus() != IntegrationStatus.ACTIVE) {
				continue;
			}
			// Evita execucoes concorrentes da mesma integracao (manual + agendada se sobrepondo),
			// que disputariam as mesmas linhas e causariam falha de lock otimista.
			if (runRepository.existsByIntegrationIdAndStatus(integration.getId(), RunStatus.RUNNING)) {
				LOG.info("Integracao {} ja esta em execucao; disparo ignorado.", integration.getCode());
				continue;
			}
			IntegrationRun run = executor.startRun(integration, triggerType);
			executor.executeRunAsync(run.getId(), false);
			started++;
		}
		if (started > 0) {
			LOG.info("Coleta do provedor via integracoes: {} integracao(oes) disparada(s).", started);
		}
		return started;
	}
}
