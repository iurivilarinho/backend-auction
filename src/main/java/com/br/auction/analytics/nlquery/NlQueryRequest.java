package com.br.auction.analytics.nlquery;

import io.swagger.v3.oas.annotations.media.Schema;

/** Pergunta livre + (opcional) HQL anterior, para refinar o resultado. */
@Schema(description = "Pergunta em linguagem natural para gerar uma consulta")
public record NlQueryRequest(
		@Schema(description = "Pergunta em linguagem natural") String question,
		@Schema(description = "HQL gerado anteriormente, para refinar") String previousSql) {
}
