package com.br.auction.notification;

/** Tipo de destino de notificacao: uma conversa individual (contato) ou um grupo do WhatsApp. */
public enum DestinationType {

	CONTACT("Contato"),
	GROUP("Grupo");

	private final String description;

	DestinationType(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
