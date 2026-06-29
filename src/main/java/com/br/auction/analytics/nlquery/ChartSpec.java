package com.br.auction.analytics.nlquery;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** Sugestão de visualização do resultado (gráfico). */
@Schema(description = "Especificação do gráfico sugerido pela IA")
public record ChartSpec(
		@Schema(description = "Tipo: bar | line | area | scatter | composed | pie | none") String type,
		@Schema(description = "Campo do eixo X") String x,
		@Schema(description = "Séries (campos do eixo Y)") List<String> series,
		@Schema(description = "Título do gráfico") String title) {
}
