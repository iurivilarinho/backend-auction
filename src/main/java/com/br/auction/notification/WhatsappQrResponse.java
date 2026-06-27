package com.br.auction.notification;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "QR / pareamento da instancia do WhatsApp")
public class WhatsappQrResponse {

	private final String base64;
	private final String pairingCode;
	private final String code;
	private final String state;
	private final String error;

	public WhatsappQrResponse(String base64, String pairingCode, String code, String state, String error) {
		this.base64 = base64;
		this.pairingCode = pairingCode;
		this.code = code;
		this.state = state;
		this.error = error;
	}

	public static WhatsappQrResponse error(String message) {
		return new WhatsappQrResponse(null, null, null, null, message);
	}

	public String getBase64() {
		return base64;
	}

	public String getPairingCode() {
		return pairingCode;
	}

	public String getCode() {
		return code;
	}

	public String getState() {
		return state;
	}

	public String getError() {
		return error;
	}
}
