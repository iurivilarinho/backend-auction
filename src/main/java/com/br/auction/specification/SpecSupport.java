package com.br.auction.specification;

import java.util.List;

/** Utilitarios compartilhados pelas specifications. */
final class SpecSupport {

	private SpecSupport() {
	}

	/** Normaliza uma lista de filtros textuais: remove nulos/brancos e faz trim. */
	static List<String> normalize(List<String> values) {
		if (values == null) {
			return List.of();
		}
		return values.stream().filter(value -> value != null && !value.isBlank()).map(String::trim).toList();
	}
}
