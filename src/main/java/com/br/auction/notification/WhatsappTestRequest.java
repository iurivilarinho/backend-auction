package com.br.auction.notification;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pedido de envio de mensagem de teste")
public class WhatsappTestRequest {

	@Schema(description = "Numero/JID de destino (opcional; usa o destino padrao se vazio)")
	private String to;

	@Schema(description = "Id de um destino cadastrado (opcional)")
	private Long destinationId;

	@Schema(description = "Mensagem (opcional; usa um texto padrao se vazio)")
	private String message;

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public Long getDestinationId() {
		return destinationId;
	}

	public void setDestinationId(Long destinationId) {
		this.destinationId = destinationId;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
