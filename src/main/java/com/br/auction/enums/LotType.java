package com.br.auction.enums;

public enum LotType {

	CONSERVADO("CONSERVADO"), SUCATA("SUCATA");

	private String description;

	private LotType(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

}
