package com.br.auction.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.br.auction.enums.AuctionProvider;
import com.br.auction.models.AuctionItem;
import com.br.auction.repository.AuctionItemRepository;
import com.br.auction.service.AuctionDetranService.LotLiveData;

/**
 * Atualiza os lotes com os dados AO VIVO do provedor (lance atual real, prazo de encerramento POR
 * LOTE e status), buscados lote a lote no endpoint do cronometro. O scraping do HTML so traz o lance
 * inicial e a data do leilao; aqui corrigimos para o que vale de verdade.
 *
 * <p>As chamadas sao espacadas (throttle) para nao martelar o provedor.
 */
@Service
public class LotLiveRefreshService {

	private static final Logger LOG = LoggerFactory.getLogger(LotLiveRefreshService.class);

	private final AuctionDetranService detranService;
	private final AuctionItemRepository itemRepository;

	public LotLiveRefreshService(AuctionDetranService detranService, AuctionItemRepository itemRepository) {
		this.detranService = detranService;
		this.itemRepository = itemRepository;
	}

	/** Resultado de uma rodada de atualizacao ao vivo. */
	public record RefreshResult(int requested, int updated, int failed) {
	}

	public RefreshResult refreshAuction(Long auctionId, long throttleMs) {
		return refreshItems(itemRepository.findByAuctionId(auctionId), throttleMs);
	}

	public RefreshResult refreshAll(long throttleMs) {
		return refreshItems(itemRepository.findAll(), throttleMs);
	}

	/**
	 * Atualiza um lote de lotes ABERTOS, priorizando os ainda sem prazo (lotes novos) e os que encerram
	 * mais cedo — que sao os que mais importam para os alertas. Mantem o volume baixo (anti-ban).
	 */
	public RefreshResult refreshOpenBatch(int batchSize, long throttleMs) {
		List<AuctionItem> open = itemRepository.findAll().stream()
				.filter(LotLiveRefreshService::isOpen)
				.sorted(Comparator.comparing(AuctionItem::getLotClosingDate,
						Comparator.nullsFirst(Comparator.naturalOrder())))
				.limit(Math.max(0, batchSize))
				.toList();
		return refreshItems(open, throttleMs);
	}

	/** Lote aberto (ainda aceita lance): status desconhecido ou diferente de encerrado (3/4). */
	private static boolean isOpen(AuctionItem item) {
		String status = item.getLotStatus();
		return status == null || !(status.equals("3") || status.equals("4"));
	}

	private RefreshResult refreshItems(List<AuctionItem> items, long throttleMs) {
		int updated = 0;
		int failed = 0;
		for (AuctionItem item : items) {
			if (refreshItem(item)) {
				updated++;
			} else {
				failed++;
			}
			pace(throttleMs);
		}
		LOG.info("Atualizacao ao vivo: {} lote(s), {} atualizado(s), {} sem dados.", items.size(), updated, failed);
		return new RefreshResult(items.size(), updated, failed);
	}

	/** Busca os dados ao vivo de um lote e grava lance atual, prazo do lote e status. */
	public boolean refreshItem(AuctionItem item) {
		Optional<LotLiveData> live = detranService.fetchLotLive(AuctionProvider.defaultProvider(), item.getLotId());
		if (live.isEmpty()) {
			return false;
		}
		LotLiveData data = live.get();
		if (data.getCurrentBid() != null) {
			item.setCurrentBidValue(data.getCurrentBid());
		}
		LocalDateTime closing = data.closingDateFrom(LocalDateTime.now());
		if (closing != null) {
			item.setLotClosingDate(closing);
		}
		if (data.getStatusLeilao() != null && !data.getStatusLeilao().isBlank()) {
			item.setLotStatus(data.getStatusLeilao());
		}
		itemRepository.save(item);
		return true;
	}

	private void pace(long throttleMs) {
		if (throttleMs <= 0) {
			return;
		}
		try {
			Thread.sleep(Duration.ofMillis(throttleMs).toMillis());
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}
}
