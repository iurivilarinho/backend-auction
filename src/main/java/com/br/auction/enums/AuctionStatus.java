package com.br.auction.enums;

import java.text.Normalizer;

public enum AuctionStatus {

	PUBLICADO("Publicado"), EM_ANDAMENTO("Em Andamento"), FINALIZADO("Finalizado");

	private final String description;

	AuctionStatus(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public static AuctionStatus fromSource(String value) {
		if (value == null || value.isBlank()) {
			return PUBLICADO;
		}

		String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toLowerCase()
				.trim();

		if (normalized.contains("andamento")) {
			return EM_ANDAMENTO;
		}
		if (normalized.contains("finalizado") || normalized.contains("encerrado")) {
			return FINALIZADO;
		}
		return PUBLICADO;
	}
}
