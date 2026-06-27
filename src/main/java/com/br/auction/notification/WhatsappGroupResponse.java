package com.br.auction.notification;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Grupo do WhatsApp do numero logado")
public class WhatsappGroupResponse {

	private final String id;
	private final String subject;

	public WhatsappGroupResponse(String id, String subject) {
		this.id = id;
		this.subject = subject;
	}

	public String getId() {
		return id;
	}

	public String getSubject() {
		return subject;
	}
}
