package com.br.auction.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Página de lotes AO VIVO do feed do provedor. Mantém o contrato {@code {lots, page, pageSize,
 * hasNext}} consumido pelo módulo de integração, agora tipado em vez de {@code Map<String, Object>}.
 */
@Schema(description = "Página de lotes ao vivo do feed do provedor")
public class LotLiveFeedPageResponse {

	@Schema(description = "Lotes ao vivo da página")
	private final List<LotLiveFeedResponse> lots;

	@Schema(description = "Número da página atual (1-based)", example = "1")
	private final int page;

	@Schema(description = "Tamanho da página", example = "50")
	private final int pageSize;

	@Schema(description = "Indica se há mais páginas na fonte", example = "true")
	private final boolean hasNext;

	public LotLiveFeedPageResponse(List<LotLiveFeedResponse> lots, int page, int pageSize, boolean hasNext) {
		this.lots = lots;
		this.page = page;
		this.pageSize = pageSize;
		this.hasNext = hasNext;
	}

	public List<LotLiveFeedResponse> getLots() {
		return lots;
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
