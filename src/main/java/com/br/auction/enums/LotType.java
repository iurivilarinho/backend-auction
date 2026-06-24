package com.br.auction.enums;

public enum LotType {

	CONSERVADO("CONSERVADO"), SUCATA("SUCATA");

	private final String description;

	LotType(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public static LotType fromSource(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		String normalized = value.toUpperCase().trim();
		if (normalized.contains("CONSERVADO")) {
			return CONSERVADO;
		}
		if (normalized.contains("SUCATA")) {
			return SUCATA;
		}
		return null;
	}
}
