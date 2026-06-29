package com.br.auction.analytics.nlquery;

import io.swagger.v3.oas.annotations.media.Schema;

/** Exportação em Excel de um resultado (reexecuta o HQL, revalidado). */
@Schema(description = "Pedido de exportação em Excel de uma consulta do B.I.")
public record ExportRequest(
		@Schema(description = "HQL a reexecutar (revalidado)") String sql,
		@Schema(description = "Nome do arquivo de saída") String fileName) {
}
