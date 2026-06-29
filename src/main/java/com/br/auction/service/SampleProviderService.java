package com.br.auction.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.br.auction.response.AuctionFeedPageResponse;
import com.br.auction.response.AuctionFeedResponse;

/**
 * Gera o feed de exemplo (offline) de leilões, paginado por cursor no mesmo contrato do feed real
 * ({@code {items, page, pageSize, hasNext}}). Concentra a montagem dos dados e a paginação para
 * manter o controller fino.
 */
@Service
public class SampleProviderService {

	private static final int TOTAL_AUCTIONS = 12;
	private static final String[] CITIES = { "Belo Horizonte", "Uberlandia", "Contagem", "Juiz de Fora", "Betim",
			"Montes Claros" };
	private static final String[] STATUSES = { "Publicado", "Em Andamento", "Publicado", "Finalizado" };

	public AuctionFeedPageResponse auctions(int page, int pageSize) {
		List<AuctionFeedResponse> all = buildAuctions();
		int from = Math.max(0, (page - 1) * pageSize);
		int to = Math.min(all.size(), from + pageSize);
		List<AuctionFeedResponse> items = from >= all.size() ? List.of() : all.subList(from, to);
		return new AuctionFeedPageResponse(items, page, pageSize, to < all.size());
	}

	private List<AuctionFeedResponse> buildAuctions() {
		List<AuctionFeedResponse> auctions = new ArrayList<>(TOTAL_AUCTIONS);
		for (int i = 1; i <= TOTAL_AUCTIONS; i++) {
			auctions.add(new AuctionFeedResponse(
					"FEED-" + String.format("%03d", i),
					String.format("%03d/2026", i),
					CITIES[(i - 1) % CITIES.length],
					"Patio Regional " + (((i - 1) % 3) + 1),
					STATUSES[(i - 1) % STATUSES.length],
					String.format("%02d/08/2026 14:00", ((i - 1) % 28) + 1),
					"2026",
					"https://leilao.detran.mg.gov.br/feed/" + i));
		}
		return auctions;
	}
}
