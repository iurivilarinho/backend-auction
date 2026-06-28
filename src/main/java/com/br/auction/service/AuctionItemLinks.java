package com.br.auction.service;

import com.br.auction.models.Auction;
import com.br.auction.models.AuctionItem;

/**
 * Constroi o link "direto pro veiculo" a partir da URL publica do leilao.
 *
 * <p>O provedor (DETRAN-MG) renderiza cada lote num card cujo atributo {@code id} e o proprio
 * {@code lotId} (ver {@code AuctionDetranService#parseLots}). Logo, ancorar a URL do leilao com
 * {@code #<lotId>} faz o navegador rolar direto ate o veiculo, em vez de abrir a lista inteira.
 * Quando nao ha lotId, cai de volta na URL do leilao.
 */
public final class AuctionItemLinks {

	private AuctionItemLinks() {
	}

	/** URL que abre o leilao ja posicionado no veiculo (ou a URL do leilao, como fallback). */
	public static String lotUrl(AuctionItem item) {
		if (item == null) {
			return null;
		}
		Auction auction = item.getAuction();
		String base = auction == null ? null : auction.getSourceUrl();
		if (base == null || base.isBlank()) {
			return base;
		}
		String lotId = item.getLotId();
		if (lotId == null || lotId.isBlank() || base.indexOf('#') >= 0) {
			return base;
		}
		return base + "#" + lotId.trim();
	}
}
