package com.br.auction.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.br.auction.models.AuctionItem;
import com.br.auction.repository.AuctionItemRepository;

/**
 * Backfill agendado e pausado da FIPE. A API publica da FIPE tem limite baixo (500-1000 req/dia), entao
 * este job preenche o valor FIPE dos itens aos poucos, em lotes pequenos, sem estourar a cota:
 *
 * <ul>
 *   <li>Processa apenas alguns itens por rodada (sem valor FIPE e ainda nao tentados no periodo).</li>
 *   <li>Marca {@code fipeCheckedAt} nos que nao tem correspondencia na FIPE, para nao ficar re-tentando
 *       sem parar os veiculos que simplesmente nao existem na tabela (ex.: reboques, marcas exoticas).</li>
 *   <li>Ao tomar 429, o {@link FipeService} entra em backoff; aqui a rodada para na hora e NAO marca o
 *       item (para tentar de novo quando a cota voltar).</li>
 * </ul>
 *
 * Tudo configuravel/desligavel por env ({@code FIPE_BACKFILL_*}). Com um token FIPE configurado, basta
 * aumentar o lote/frequencia para preencher mais rapido.
 */
@Component
public class FipeBackfillScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(FipeBackfillScheduler.class);

	private final AuctionItemRepository itemRepository;
	private final FipeService fipeService;
	private final boolean enabled;
	private final int batchSize;
	private final int retryDays;

	public FipeBackfillScheduler(AuctionItemRepository itemRepository, FipeService fipeService,
			@Value("${fipe.backfill.enabled:true}") boolean enabled,
			@Value("${fipe.backfill.batch-size:2}") int batchSize,
			@Value("${fipe.backfill.retry-days:14}") int retryDays) {
		this.itemRepository = itemRepository;
		this.fipeService = fipeService;
		this.enabled = enabled;
		this.batchSize = Math.max(1, batchSize);
		this.retryDays = Math.max(1, retryDays);
	}

	@Scheduled(fixedDelayString = "${fipe.backfill.interval-ms:1800000}", initialDelayString = "${fipe.backfill.initial-delay-ms:120000}")
	public void runScheduled() {
		if (!enabled || fipeService.isRateLimited()) {
			return;
		}
		try {
			runBatch();
		} catch (RuntimeException ex) {
			LOG.warn("Falha na rodada de backfill FIPE: {}", ex.getMessage());
		}
	}

	/** Processa um lote de itens pendentes de FIPE. Retorna a quantidade preenchida. */
	public int runBatch() {
		LocalDateTime cutoff = LocalDateTime.now().minusDays(retryDays);
		List<AuctionItem> items = itemRepository.findFipeBackfillCandidates(cutoff, PageRequest.of(0, batchSize));
		if (items.isEmpty()) {
			return 0;
		}
		int filled = 0;
		int checked = 0;
		for (AuctionItem item : items) {
			BigDecimal value = fipeService.getFipeValue(item.getVehicleDescription(), item.getVehicleYear());
			if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
				item.setFipeValue(value);
				item.setFipeCheckedAt(LocalDateTime.now());
				itemRepository.save(item);
				filled++;
			} else if (fipeService.isRateLimited()) {
				// 429 no meio do lote: para e NAO marca o item (tenta de novo quando a cota voltar).
				break;
			} else {
				// Sem correspondencia na FIPE: marca para nao re-tentar sem parar.
				item.setFipeCheckedAt(LocalDateTime.now());
				itemRepository.save(item);
				checked++;
			}
		}
		if (filled > 0 || checked > 0) {
			LOG.info("Backfill FIPE: {} preenchido(s), {} sem correspondencia (lote de {}).", filled, checked,
					items.size());
		}
		return filled;
	}
}
