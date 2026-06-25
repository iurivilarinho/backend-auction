package com.br.auction.garage.acquisition;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado da importacao de arremates")
public record ArrematesImportResult(
		@Schema(description = "Quantidade importada") int imported,
		@Schema(description = "Quantidade ignorada (duplicada)") int skipped,
		@Schema(description = "Mensagem") String message) {
}
