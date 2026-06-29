package com.br.auction.analytics.nlquery;

import io.swagger.v3.oas.annotations.media.Schema;

/** Recarrega uma visão salva: reexecuta o HQL (revalidado) com o gráfico e o limite salvos. */
@Schema(description = "Pedido de reexecução de um HQL salvo para recarregar uma visão")
public record RunRequest(
		@Schema(description = "HQL a reexecutar (revalidado)") String sql,
		@Schema(description = "Gráfico salvo") ChartSpec chart,
		@Schema(description = "Título da visão") String title,
		@Schema(description = "\"top N\" salvo") Integer limit) {
}
