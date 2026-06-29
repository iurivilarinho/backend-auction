package com.br.auction.analytics.nlquery;

import io.swagger.v3.oas.annotations.media.Schema;

/** Metadado de uma coluna do resultado. */
@Schema(description = "Metadado de uma coluna do resultado")
public record ColumnMeta(
		@Schema(description = "Nome da coluna") String name,
		@Schema(description = "Tipo da coluna") String type) {
}
