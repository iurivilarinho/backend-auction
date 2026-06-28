package com.br.auction.service;

import com.br.auction.enums.AuctionProvider;
import com.br.auction.models.Auction;
import com.br.auction.models.AuctionItem;

/**
 * Constroi o link "direto pro veiculo" no provedor.
 *
 * <p>No DETRAN-MG a pagina do lote e {@code <baseUrl>/lotes/detalhes/<lotId>} (e o destino do clique
 * no card, ver {@code AuctionDetranService}). Abrir essa URL leva direto ao veiculo, em vez da lista
 * inteira do edital. Quando nao ha lotId, cai de volta na URL do leilao.
 */
public final class AuctionItemLinks {

	private AuctionItemLinks() {
	}

	/** URL da pagina do veiculo no provedor (ou a URL do leilao, como fallback). */
	public static String lotUrl(AuctionItem item) {
		if (item == null) {
			return null;
		}
		Auction auction = item.getAuction();
		String lotId = item.getLotId();
		if (lotId == null || lotId.isBlank()) {
			return auction == null ? null : auction.getSourceUrl();
		}
		String providerCode = auction == null ? null : auction.getProviderCode();
		String baseUrl = AuctionProvider.fromCodeOrDefault(providerCode).getBaseUrl();
		return baseUrl + "/lotes/detalhes/" + lotId.trim();
	}
}
