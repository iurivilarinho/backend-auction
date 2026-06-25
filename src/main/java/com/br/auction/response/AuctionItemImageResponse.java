package com.br.auction.response;

import com.br.auction.models.AuctionItemImage;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Referencia publica a uma imagem armazenada. A URL aponta para o proprio backend
 * (servida do banco), portanto continua valida mesmo que o provedor saia do ar.
 */
@Schema(description = "Referencia a uma imagem de veiculo servida pelo backend")
public class AuctionItemImageResponse {

	@Schema(description = "Identificador da imagem")
	private final Long id;

	@Schema(description = "URL servida pelo backend (a partir do banco)")
	private final String url;

	@Schema(description = "URL original no provedor")
	private final String sourceUrl;

	public AuctionItemImageResponse(AuctionItemImage image) {
		this.id = image.getId();
		this.url = "/api/images/" + image.getId();
		this.sourceUrl = image.getSourceUrl();
	}

	public Long getId() {
		return id;
	}

	public String getUrl() {
		return url;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}
}
