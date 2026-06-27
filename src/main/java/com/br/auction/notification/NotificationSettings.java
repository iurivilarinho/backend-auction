package com.br.auction.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Configuracao do canal de notificacao editavel em runtime pela tela do app (liga/desliga e numero
 * de destino). Linha unica (id fixo). Segredos de infra (base-url, api-key, instancia da Evolution)
 * continuam vindo de variavel de ambiente — aqui ficam so as preferencias do usuario.
 */
@Entity
@Table(name = "tbNotificationSettings")
@Schema(description = "Configuracao do canal de notificacao (WhatsApp)")
public class NotificationSettings {

	public static final Long SINGLETON_ID = 1L;

	@Id
	@Schema(description = "Identificador fixo (linha unica)")
	private Long id = SINGLETON_ID;

	@Column(nullable = false)
	@Schema(description = "Canal WhatsApp ligado")
	private Boolean enabled = Boolean.FALSE;

	@Column(length = 30)
	@Schema(description = "Numero de destino global (E.164 sem +), ex.: 5534999998888")
	private String recipient;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public String getRecipient() {
		return recipient;
	}

	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}
}
