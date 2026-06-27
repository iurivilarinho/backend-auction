package com.br.auction.notification;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Preferencias do canal WhatsApp")
public class WhatsappSettingsRequest {

	@Schema(description = "Canal ligado (envia notificacoes)")
	private Boolean enabled;

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}
}
