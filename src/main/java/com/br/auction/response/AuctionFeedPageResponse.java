package com.br.auction.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Página de leilões do feed do provedor. Mantém o contrato {@code {items, page, pageSize, hasNext}}
 * consumido pelo módulo de integração (o feed imita um provedor REST paginado por cursor), agora
 * tipado em vez de {@code Map<String, Object>}.
 */
@Schema(description = "Página de leilões do feed do provedor")
public class AuctionFeedPageResponse {

	@Schema(description = "Leilões da página")
	private final List<AuctionFeedResponse> items;

	@Schema(description = "Número da página atual (1-based)", example = "1")
	private final int page;

	@Schema(description = "Tamanho da página", example = "100")
	private final int pageSize;

	@Schema(description = "Indica se há mais páginas na fonte", example = "true")
	private final boolean hasNext;

	public AuctionFeedPageResponse(List<AuctionFeedResponse> items, int page, int pageSize, boolean hasNext) {
		this.items = items;
		this.page = page;
		this.pageSize = pageSize;
		this.hasNext = hasNext;
	}

	public List<AuctionFeedResponse> getItems() {
		return items;
	}

	public int getPage() {
		return page;
	}

	public int getPageSize() {
		return pageSize;
	}

	public boolean isHasNext() {
		return hasNext;
	}
}
