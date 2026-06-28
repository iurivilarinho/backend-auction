package com.br.auction.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.br.auction.service.LotLiveRefreshService.RefreshResult;

/**
 * Mantem o lance ao vivo / prazo / status dos lotes atualizados. A integracao (scraping) so traz o
 * lance INICIAL; o lance real vem do endpoint por lote. Para nao martelar o provedor, a cada rodada
 * atualiza um lote de lotes abertos, priorizando os sem prazo (novos) e os que encerram mais cedo.
 */
@Component
public class LotLiveScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(LotLiveScheduler.class);

	private final LotLiveRefreshService refreshService;
	private final boolean enabled;
	private final int batchSize;
	private final long throttleMs;

	public LotLiveScheduler(LotLiveRefreshService refreshService,
			@Value("${lot.live.enabled:true}") boolean enabled,
			@Value("${lot.live.batch-size:150}") int batchSize,
			@Value("${lot.live.throttle-ms:300}") long throttleMs) {
		this.refreshService = refreshService;
		this.enabled = enabled;
		this.batchSize = batchSize;
		this.throttleMs = throttleMs;
	}

	@Scheduled(fixedDelayString = "${lot.live.interval-ms:600000}", initialDelayString = "${lot.live.initial-delay-ms:120000}")
	public void refreshOpenLots() {
		if (!enabled) {
			return;
		}
		try {
			RefreshResult result = refreshService.refreshOpenBatch(batchSize, throttleMs);
			if (result.updated() > 0 || result.failed() > 0) {
				LOG.info("Refresh ao vivo: {} lote(s) atualizado(s), {} sem dados.", result.updated(), result.failed());
			}
		} catch (RuntimeException ex) {
			LOG.warn("Falha no refresh ao vivo dos lotes: {}", ex.getMessage());
		}
	}
}
