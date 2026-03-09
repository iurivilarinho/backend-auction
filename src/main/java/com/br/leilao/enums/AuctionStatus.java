package com.br.leilao.enums;

public enum AuctionStatus {

	PUBLICADO("Publicado"), EM_ANDAMENTO("Em Andamento"), FINALIZADO("Finalizado");

	private String description;

	private AuctionStatus(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

}
