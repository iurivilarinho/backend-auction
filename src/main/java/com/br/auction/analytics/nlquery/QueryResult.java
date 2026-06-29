package com.br.auction.analytics.nlquery;

import java.util.List;

/**
 * Resultado da execução (colunas + linhas alinhadas às colunas). Value object interno do motor
 * text-to-HQL; não é exposto diretamente no contrato (os dados vão achatados em {@link NlQueryResponse}).
 */
public record QueryResult(List<ColumnMeta> columns, List<List<Object>> rows) {
}
