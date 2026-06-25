package com.br.auction.analytics.nlquery;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** DTOs do assistente text-to-HQL do B.I. */
public final class NlQueryDtos {

    private NlQueryDtos() {
    }

    /** Pergunta livre + (opcional) HQL anterior, para refinar o resultado. */
    @Schema(name = "NlQueryRequest", description = "Pergunta em linguagem natural para gerar uma consulta.")
    public record NlQueryRequest(String question, String previousSql) {
    }

    /** Sugestao de visualizacao do resultado (grafico). */
    @Schema(name = "NlQueryChart", description = "Especificacao do grafico sugerido pela IA.")
    public record ChartSpec(
            /** bar | line | area | scatter | composed | pie | none */
            String type,
            String x,
            List<String> series,
            String title) {
    }

    /** Metadado de uma coluna do resultado. */
    @Schema(name = "NlQueryColumn")
    public record ColumnMeta(String name, String type) {
    }

    /** Resultado da execucao (colunas + linhas alinhadas as colunas). */
    public record QueryResult(List<ColumnMeta> columns, List<List<Object>> rows) {
    }

    /** Resposta completa ao usuario. */
    @Schema(name = "NlQueryResponse", description = "Resultado do assistente text-to-HQL.")
    public record NlQueryResponse(
            String question,
            String sql,
            String explanation,
            ChartSpec chart,
            List<ColumnMeta> columns,
            List<List<Object>> rows,
            int rowCount,
            boolean fromAi,
            /** true = a IA esta fora do ar. */
            boolean aiOffline,
            /** Resposta conversacional da IA (apresenta o resultado, recusa ou sugere). */
            String message) {
    }

    /** Disponibilidade do provedor de IA (para a tela escolher o modo). */
    @Schema(name = "NlQueryHealth")
    public record HealthResponse(boolean available) {
    }

    /** Exportacao em Excel de um resultado (reexecuta o HQL, revalidado). */
    @Schema(name = "NlQueryExportRequest")
    public record ExportRequest(String sql, String fileName) {
    }

    /** Recarrega uma visao salva: reexecuta o HQL (revalidado) com o grafico salvo. */
    @Schema(name = "NlQueryRunRequest")
    public record RunRequest(String sql, ChartSpec chart, String title) {
    }
}
