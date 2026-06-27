package com.br.auction.garage.alert;

import com.br.auction.garage.enums.AlertType;
import com.br.auction.garage.models.VehicleAlert;
import com.br.auction.models.AuctionItem;
import com.br.auction.response.AuctionItemResponse;

import io.swagger.v3.oas.annotations.media.Schema;

/** Um veiculo encontrado por um alerta (para a tela de Achados). */
@Schema(description = "Achado de um alerta: o veiculo correspondente + de qual alerta veio")
public class AlertFindingResponse {

	private final Long alertId;
	private final String alertName;
	private final AlertType alertType;
	private final String alertTypeLabel;
	private final boolean notified;
	private final Double distanceKm;
	private final AuctionItemResponse vehicle;

	public AlertFindingResponse(VehicleAlert alert, AuctionItem item, boolean notified, Double distanceKm) {
		this.alertId = alert.getId();
		this.alertName = alert.getName();
		this.alertType = alert.getType();
		this.alertTypeLabel = alert.getType() == null ? null : alert.getType().getDescription();
		this.notified = notified;
		this.distanceKm = distanceKm;
		this.vehicle = new AuctionItemResponse(item);
	}

	public Long getAlertId() {
		return alertId;
	}

	public String getAlertName() {
		return alertName;
	}

	public AlertType getAlertType() {
		return alertType;
	}

	public String getAlertTypeLabel() {
		return alertTypeLabel;
	}

	public boolean isNotified() {
		return notified;
	}

	public Double getDistanceKm() {
		return distanceKm;
	}

	public AuctionItemResponse getVehicle() {
		return vehicle;
	}
}
