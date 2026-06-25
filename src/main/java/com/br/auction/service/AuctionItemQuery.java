package com.br.auction.service;

import java.math.BigDecimal;
import java.util.List;

import com.br.auction.enums.AuctionStatus;
import com.br.auction.enums.LotType;

/**
 * Agrupa os parametros de filtragem de itens de leilao. Mantido como um objeto unico para
 * evitar metodos com lista extensa de argumentos e facilitar a inclusao de novos filtros.
 */
public record AuctionItemQuery(
		Long auctionId,
		List<AuctionStatus> auctionStatus,
		List<LotType> type,
		String search,
		List<String> brands,
		List<String> years,
		String model,
		BigDecimal minBid,
		BigDecimal maxBid,
		BigDecimal minFipe,
		BigDecimal maxFipe,
		Boolean closed,
		String providerCode,
		String stateCode) {
}
