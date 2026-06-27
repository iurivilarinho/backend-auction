package com.br.auction.notification;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status do canal e da conexao do WhatsApp")
public class WhatsappStatusResponse {

	private final boolean enabled;
	private final boolean hasApiKey;
	private final boolean configured;
	private final String instance;
	private final String connectionState;

	public WhatsappStatusResponse(boolean enabled, boolean hasApiKey, boolean configured, String instance,
			String connectionState) {
		this.enabled = enabled;
		this.hasApiKey = hasApiKey;
		this.configured = configured;
		this.instance = instance;
		this.connectionState = connectionState;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isHasApiKey() {
		return hasApiKey;
	}

	public boolean isConfigured() {
		return configured;
	}

	public String getInstance() {
		return instance;
	}

	public String getConnectionState() {
		return connectionState;
	}
}
