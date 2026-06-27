package com.br.auction.notification;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Destino de notificacao do WhatsApp: uma conversa individual (numero) ou um grupo (JID). Os alertas
 * sao enviados para todos os destinos habilitados; cada alerta pode ainda sobrescrever com um numero
 * proprio. Gerenciado pela tela de Parametros.
 */
@Entity
@Table(name = "tbNotificationDestination")
@Schema(description = "Destino de notificacao (contato ou grupo do WhatsApp)")
public class NotificationDestination {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	@Schema(description = "Rotulo/nome amigavel do destino")
	private String label;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	@Schema(description = "Tipo do destino: CONTACT (numero) ou GROUP (grupo)")
	private DestinationType type = DestinationType.CONTACT;

	@Column(nullable = false, length = 80)
	@Schema(description = "Valor de envio: numero E.164 sem + (contato) ou JID do grupo (...@g.us)")
	private String value;

	@Column(nullable = false)
	@Schema(description = "Recebe alertas quando habilitado")
	private Boolean enabled = Boolean.TRUE;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	public void onCreate() {
		this.createdAt = LocalDateTime.now();
		if (this.enabled == null) {
			this.enabled = Boolean.TRUE;
		}
		if (this.type == null) {
			this.type = DestinationType.CONTACT;
		}
	}

	public Long getId() {
		return id;
	}

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

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
