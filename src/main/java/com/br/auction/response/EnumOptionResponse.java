package com.br.auction.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Opção de enum para montar selects/filtros no frontend: valor técnico (nome do enum) + rótulo
 * amigável. Substitui os antigos {@code Map.of("value", ..., "label", ...)} por um modelo tipado.
 */
@Schema(description = "Opção de enum (valor técnico + rótulo amigável)")
public class EnumOptionResponse {

	@Schema(description = "Valor técnico (nome do enum)", example = "SUCCESS")
	private final String value;

	@Schema(description = "Rótulo amigável para exibição", example = "Concluída")
	private final String label;

	public EnumOptionResponse(String value, String label) {
		this.value = value;
		this.label = label;
	}

	public String getValue() {
		return value;
	}

	public String getLabel() {
		return label;
	}
}
