package com.br.auction.analytics.nlquery;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** Resposta completa ao usuário do assistente text-to-HQL. */
@Schema(description = "Resultado do assistente text-to-HQL")
public record NlQueryResponse(
		@Schema(description = "Pergunta original") String question,
		@Schema(description = "HQL gerado") String sql,
		@Schema(description = "Explicação da consulta") String explanation,
		@Schema(description = "Gráfico sugerido") ChartSpec chart,
		@Schema(description = "Colunas do resultado") List<ColumnMeta> columns,
		@Schema(description = "Linhas do resultado (alinhadas às colunas)") List<List<Object>> rows,
		@Schema(description = "Quantidade de linhas") int rowCount,
		@Schema(description = "Indica se a resposta veio da IA") boolean fromAi,
		@Schema(description = "Indica se a IA está fora do ar") boolean aiOffline,
		@Schema(description = "Resposta conversacional da IA") String message,
		@Schema(description = "\"top N\" aplicado, para reexecutar a visão salva igual") Integer limit) {
}
