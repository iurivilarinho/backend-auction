package com.br.auction.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.br.auction.enums.AuctionProvider;
import com.br.auction.filter.AuctionSourceFilter;
import com.br.auction.models.AuctionItem;
import com.br.auction.repository.AuctionItemRepository;
import com.br.auction.response.AuctionFeedPageResponse;
import com.br.auction.response.AuctionFeedResponse;
import com.br.auction.response.AuctionJsonResponse;
import com.br.auction.response.AuctionListJsonResponse;
import com.br.auction.response.LotFeedPageResponse;
import com.br.auction.response.LotFeedResponse;
import com.br.auction.response.LotLiveFeedPageResponse;
import com.br.auction.response.LotLiveFeedResponse;
import com.br.auction.response.LotResponse;
import com.br.auction.service.AuctionDetranService.LotLiveData;

/**
 * Orquestra o feed do provedor: escolhe o adapter conforme o provedor, mapeia para os modelos
 * tipados do feed e pagina por cursor ({@code {items|lots, page, pageSize, hasNext}}). Mantém o
 * controller fino — toda a lógica (seleção de fonte, paginação, coleta ao vivo) vive aqui.
 */
@Service
public class ProviderFeedService {

	private final AuctionDetranService detranService;
	private final LeiloService leiloService;
	private final McLeilaoService mcLeilaoService;
	private final AuctionItemRepository auctionItemRepository;
	private final int maxAuctionsForLots;
	private final int liveMaxPerRun;
	private final long liveThrottleMs;

	public ProviderFeedService(AuctionDetranService detranService, LeiloService leiloService,
			McLeilaoService mcLeilaoService, AuctionItemRepository auctionItemRepository,
			@Value("${auction.feed.max-auctions:3}") int maxAuctionsForLots,
			@Value("${lot.live.max-per-run:300}") int liveMaxPerRun,
			@Value("${lot.live.throttle-ms:200}") long liveThrottleMs) {
		this.detranService = detranService;
		this.leiloService = leiloService;
		this.mcLeilaoService = mcLeilaoService;
		this.auctionItemRepository = auctionItemRepository;
		this.maxAuctionsForLots = maxAuctionsForLots;
		this.liveMaxPerRun = liveMaxPerRun;
		this.liveThrottleMs = liveThrottleMs;
	}

	/** Leilões reais do provedor, paginados por cursor. */
	public AuctionFeedPageResponse auctions(String providerCode, int page, int pageSize) throws IOException {
		AuctionProvider provider = AuctionProvider.fromCodeOrDefault(providerCode);
		List<AuctionListJsonResponse> auctions = fetchAuctions(provider);

		int from = Math.max(0, (page - 1) * pageSize);
		int to = Math.min(auctions.size(), from + pageSize);
		List<AuctionFeedResponse> items = new ArrayList<>();
		if (from < auctions.size()) {
			for (AuctionListJsonResponse auction : auctions.subList(from, to)) {
				items.add(new AuctionFeedResponse(auction));
			}
		}
		return new AuctionFeedPageResponse(items, page, pageSize, to < auctions.size());
	}

	/** Lotes reais do provedor, paginados por cursor. */
	public LotFeedPageResponse lots(String providerCode, String auctionId, String auctionYear, int page, int pageSize)
			throws IOException {
		AuctionProvider provider = AuctionProvider.fromCodeOrDefault(providerCode);

		// LEILO: lotes vem da API publica (busca-elastic), ja filtrados (veiculos/orgao) e paginados.
		if (provider == AuctionProvider.LEILO_GO) {
			return leiloService.fetchLotsPage(provider, page, pageSize);
		}

		// MC LEILAO: lotes de veiculo (com detalhe) de todos os leiloes, paginados em memoria.
		if (provider == AuctionProvider.MCLEILAO_GO) {
			return pageInMemory(mcLeilaoService.fetchVehicleLots(provider), page, pageSize);
		}

		// Lotes ja achatados (com o id do leilao pai) de um ou mais leiloes.
		List<LotFeedResponse> allLots = new ArrayList<>();
		if (auctionId != null && !auctionId.isBlank()) {
			collectLots(provider, auctionId, auctionYear, allLots);
		} else {
			List<AuctionListJsonResponse> auctions = detranService.fetchAuctions(provider, new AuctionSourceFilter());
			int limit = maxAuctionsForLots <= 0 ? auctions.size() : Math.min(maxAuctionsForLots, auctions.size());
			for (int i = 0; i < limit; i++) {
				AuctionListJsonResponse auction = auctions.get(i);
				collectLots(provider, auction.getAuctionId(), auction.getAuctionYear(), allLots);
			}
		}
		return pageInMemory(allLots, page, pageSize);
	}

	/** Dados AO VIVO dos lotes abertos, priorizados e paginados por cursor. */
	public LotLiveFeedPageResponse lotsLive(String providerCode, int page, int pageSize) {
		AuctionProvider provider = AuctionProvider.fromCodeOrDefault(providerCode);

		// Apenas itens DESTE provedor: o feed ao-vivo bate no cronometro do provedor (DETRAN), entao
		// itens de outro provedor (ex.: LEILO) nao tem leilao pai correspondente e quebrariam o sink.
		// Lotes abertos priorizados: sem prazo (novos) primeiro, depois os que encerram mais cedo.
		List<AuctionItem> open = auctionItemRepository.findByAuctionProviderCode(provider.getCode()).stream()
				.filter(ProviderFeedService::isOpen)
				.sorted(Comparator.comparing(AuctionItem::getLotClosingDate,
						Comparator.nullsFirst(Comparator.naturalOrder())))
				.toList();

		int cap = Math.min(open.size(), Math.max(0, liveMaxPerRun));
		int from = Math.max(0, (page - 1) * pageSize);
		int to = Math.min(cap, from + pageSize);

		List<LotLiveFeedResponse> lots = new ArrayList<>();
		for (int i = from; i < to; i++) {
			AuctionItem item = open.get(i);
			Optional<LotLiveData> live = detranService.fetchLotLive(provider, item.getLotId());
			if (live.isPresent()) {
				lots.add(toLiveLot(item, live.get()));
			}
			pace(liveThrottleMs);
		}
		return new LotLiveFeedPageResponse(lots, page, pageSize, to < cap);
	}

	private List<AuctionListJsonResponse> fetchAuctions(AuctionProvider provider) throws IOException {
		if (provider == AuctionProvider.LEILO_GO) {
			return leiloService.fetchAuctions(provider);
		}
		if (provider == AuctionProvider.MCLEILAO_GO) {
			return mcLeilaoService.fetchAuctions(provider);
		}
		return detranService.fetchAuctions(provider, new AuctionSourceFilter());
	}

	private void collectLots(AuctionProvider provider, String auctionId, String auctionYear,
			List<LotFeedResponse> target) throws IOException {
		AuctionJsonResponse details = detranService.fetchAuctionLots(provider, auctionId, auctionYear);
		for (LotResponse lot : details.getLots()) {
			target.add(new LotFeedResponse(lot, auctionId));
		}
	}

	private LotLiveFeedResponse toLiveLot(AuctionItem item, LotLiveData data) {
		String auctionId = item.getAuction() == null ? null : item.getAuction().getDetranAuctionId();
		// Lance ao vivo; sem lance (valor nulo), mostra o piso/inicial para a UI nao ficar zerada.
		BigDecimal bid = data.getCurrentBid();
		if (bid == null) {
			bid = item.getMinimumBidValue() != null ? item.getMinimumBidValue() : item.getCurrentBidValue();
		}
		String currentBidValue = bid == null ? null : bid.toPlainString();
		LocalDateTime closing = data.closingDateFrom(LocalDateTime.now());
		String closingDate = closing == null ? null : closing.toString();
		return new LotLiveFeedResponse(auctionId, item.getLotId(), currentBidValue, closingDate,
				data.getStatusLeilao());
	}

	private static LotFeedPageResponse pageInMemory(List<LotFeedResponse> all, int page, int pageSize) {
		int from = Math.max(0, (page - 1) * pageSize);
		int to = Math.min(all.size(), from + pageSize);
		List<LotFeedResponse> items = from >= all.size() ? List.of() : all.subList(from, to);
		return new LotFeedPageResponse(items, page, pageSize, to < all.size());
	}

	/** Lote aberto (aceita lance): status desconhecido ou diferente de encerrado (3/4). */
	private static boolean isOpen(AuctionItem item) {
		String status = item.getLotStatus();
		return status == null || !(status.equals("3") || status.equals("4"));
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
