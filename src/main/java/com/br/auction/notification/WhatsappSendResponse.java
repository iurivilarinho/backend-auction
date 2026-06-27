package com.br.auction.notification;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado de um envio (teste) pelo canal WhatsApp")
public class WhatsappSendResponse {

	private final String status;
	private final String detail;

	public WhatsappSendResponse(WhatsappNotifier.SendResult result) {
		this.status = result.getStatus().name();
		this.detail = result.getDetail();
	}

	public String getStatus() {
		return status;
	}

	public String getDetail() {
		return detail;
	}
}
