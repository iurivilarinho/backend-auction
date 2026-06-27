package com.br.auction.notification;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Destino de notificacao")
public class NotificationDestinationResponse {

	private final Long id;
	private final String label;
	private final DestinationType type;
	private final String typeLabel;
	private final String value;
	private final boolean enabled;

	public NotificationDestinationResponse(NotificationDestination destination) {
		this.id = destination.getId();
		this.label = destination.getLabel();
		this.type = destination.getType();
		this.typeLabel = destination.getType() == null ? null : destination.getType().getDescription();
		this.value = destination.getValue();
		this.enabled = Boolean.TRUE.equals(destination.getEnabled());
	}

	public Long getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}

	public DestinationType getType() {
		return type;
	}

	public String getTypeLabel() {
		return typeLabel;
	}

	public String getValue() {
		return value;
	}

	public boolean isEnabled() {
		return enabled;
	}
}
