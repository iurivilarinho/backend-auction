package com.br.auction.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados de um destino de notificacao (contato ou grupo)")
public class NotificationDestinationRequest {

	@Schema(description = "Rotulo/nome amigavel do destino")
	private String label;

	@Schema(description = "Tipo do destino: CONTACT (numero) ou GROUP (grupo)")
	private DestinationType type;

	@NotBlank(message = "O destino (numero ou grupo) e obrigatorio")
	@Schema(description = "Numero E.164 sem + (contato) ou JID do grupo (...@g.us)")
	private String value;

	@Schema(description = "Recebe alertas quando habilitado")
	private Boolean enabled;

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public DestinationType getType() {
		return type;
	}

	public void setType(DestinationType type) {
		this.type = type;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}
}
