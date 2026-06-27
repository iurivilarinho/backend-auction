package com.br.auction.garage.models;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Registro de um disparo de alerta. Serve de trava de deduplicacao: a chave (alerta + triggerKey)
 * e unica, entao um mesmo gatilho (ex.: "lote 123 encerrando") nunca dispara duas vezes, mesmo que
 * o agendador rode a cada poucos minutos. Tambem funciona como historico/auditoria de envios.
 */
@Entity
@Table(name = "tbAlertNotification", uniqueConstraints = @UniqueConstraint(name = "UK_ALERTNOTIFICATION_TRIGGER", columnNames = {
		"fk_Id_VehicleAlert", "triggerKey" }), indexes = @Index(name = "IX_ALERTNOTIFICATION_ALERT", columnList = "fk_Id_VehicleAlert"))
@Schema(description = "Disparo de alerta (trava de deduplicacao + historico)")
public class AlertNotification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "fk_Id_VehicleAlert", nullable = false)
	@Schema(description = "Alerta que disparou")
	private Long alertId;

	@Column
	@Schema(description = "Item (lote) que disparou, quando aplicavel")
	private Long auctionItemId;

	@Column(nullable = false, length = 200)
	@Schema(description = "Chave do gatilho (tipo + lote + bucket) que garante disparo unico")
	private String triggerKey;

	@Column(nullable = false, length = 20)
	@Schema(description = "Resultado do envio: SENT, SKIPPED ou FAILED")
	private String status;

	@Column(length = 500)
	@Schema(description = "Detalhe (motivo do skip/erro ou confirmacao de envio)")
	private String detail;

	@Column(nullable = false)
	private LocalDateTime firedAt;

	@PrePersist
	public void onCreate() {
		this.firedAt = LocalDateTime.now();
	}

	public AlertNotification() {
	}

	public AlertNotification(Long alertId, Long auctionItemId, String triggerKey, String status, String detail) {
		this.alertId = alertId;
		this.auctionItemId = auctionItemId;
		this.triggerKey = triggerKey;
		this.status = status;
		this.detail = detail;
	}

	public Long getId() {
		return id;
	}

	public Long getAlertId() {
		return alertId;
	}

	public void setAlertId(Long alertId) {
		this.alertId = alertId;
	}

	public Long getAuctionItemId() {
		return auctionItemId;
	}

	public void setAuctionItemId(Long auctionItemId) {
		this.auctionItemId = auctionItemId;
	}

	public String getTriggerKey() {
		return triggerKey;
	}

	public void setTriggerKey(String triggerKey) {
		this.triggerKey = triggerKey;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}

	public LocalDateTime getFiredAt() {
		return firedAt;
	}
}
