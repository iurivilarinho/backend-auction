package com.br.auction.garage.alert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.br.auction.garage.models.AlertNotification;
import com.br.auction.garage.models.VehicleAlert;
import com.br.auction.garage.repository.AlertNotificationRepository;
import com.br.auction.garage.repository.VehicleAlertRepository;
import com.br.auction.models.Auction;
import com.br.auction.models.AuctionItem;
import com.br.auction.notification.WhatsappNotifier;
import com.br.auction.notification.WhatsappNotifier.SendResult;
import com.br.auction.service.DistanceService;

/**
 * Coracao do motor de alertas: para cada alerta ativo, seleciona os lotes candidatos, aplica o
 * gatilho do tipo, filtra por raio, deduplica (tbAlertNotification) e dispara a notificacao por
 * WhatsApp. Espacar os envios e limitar por rodada sao medidas anti-ban.
 */
@Service
public class AlertEvaluator {

	private static final Logger LOG = LoggerFactory.getLogger(AlertEvaluator.class);
	private static final NumberFormat BRL = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM 'as' HH'h'mm");
	private static final int DEFAULT_LEAD_MINUTES = 60;
	private static final int DEFAULT_FIPE_PERCENT = 80;

	private final VehicleAlertRepository alertRepository;
	private final VehicleAlertService alertService;
	private final AlertNotificationRepository notificationRepository;
	private final DistanceService distanceService;
	private final WhatsappNotifier notifier;
	private final int maxPerRun;
	private final long sendDelayMs;

	public AlertEvaluator(VehicleAlertRepository alertRepository, VehicleAlertService alertService,
			AlertNotificationRepository notificationRepository, DistanceService distanceService,
			WhatsappNotifier notifier,
			@Value("${whatsapp.alert.max-per-run:10}") int maxPerRun,
			@Value("${whatsapp.alert.send-delay-ms:2500}") long sendDelayMs) {
		this.alertRepository = alertRepository;
		this.alertService = alertService;
		this.notificationRepository = notificationRepository;
		this.distanceService = distanceService;
		this.notifier = notifier;
		this.maxPerRun = maxPerRun;
		this.sendDelayMs = sendDelayMs;
	}

	/** Avalia todos os alertas ativos. Nao lanca — falhas sao registradas e a varredura continua. */
	public void evaluateAll() {
		if (!notifier.isConfigured()) {
			LOG.debug("Avaliacao de alertas ignorada: canal WhatsApp nao configurado.");
			return;
		}
		LocalDateTime now = LocalDateTime.now();
		for (VehicleAlert alert : alertRepository.findByActiveTrue()) {
			try {
				evaluateAlert(alert, now);
			} catch (RuntimeException ex) {
				LOG.warn("Falha ao avaliar alerta {} ({}): {}", alert.getId(), alert.getName(), ex.getMessage());
			}
		}
	}

	private void evaluateAlert(VehicleAlert alert, LocalDateTime now) {
		int sent = 0;
		int candidates = 0;
		for (AuctionItem item : alertService.findCandidates(alert)) {
			if (!matchesRadius(alert, item)) {
				continue;
			}
			if (!triggers(alert, item, now)) {
				continue;
			}
			candidates++;
			String triggerKey = alert.getType().name() + ":" + item.getId();
			if (notificationRepository.existsByAlertIdAndTriggerKey(alert.getId(), triggerKey)) {
				continue;
			}
			if (sent >= maxPerRun) {
				LOG.info("Alerta {} atingiu o limite de {} envios nesta rodada; restante fica para a proxima.",
						alert.getId(), maxPerRun);
				break;
			}
			SendResult result = notifier.send(recipient(alert), buildMessage(alert, item));
			if (result.isSent()) {
				notificationRepository.save(new AlertNotification(alert.getId(), item.getId(), triggerKey,
						result.getStatus().name(), result.getDetail()));
				sent++;
				pace();
			} else {
				// Nao registra: permite reenvio na proxima rodada quando o canal voltar.
				LOG.warn("Alerta {} item {} nao enviado: {}", alert.getId(), item.getId(), result.getDetail());
			}
		}
		if (sent > 0) {
			LOG.info("Alerta {} ({}): {} notificacao(oes) enviada(s) de {} gatilho(s).",
					alert.getId(), alert.getName(), sent, candidates);
		}
	}

	// ---------------------------------- Gatilhos por tipo ----------------------------------

	private boolean triggers(VehicleAlert alert, AuctionItem item, LocalDateTime now) {
		switch (alert.getType()) {
		case NEW_MATCH:
			return true;
		case CLOSING_SOON:
			return triggersClosingSoon(alert, item, now);
		case PRICE_ABOVE:
			return item.getCurrentBidValue() != null && alert.getThresholdValue() != null
					&& item.getCurrentBidValue().compareTo(alert.getThresholdValue()) > 0;
		case SOLD_BELOW:
			return triggersSoldBelow(alert, item, now);
		case FIPE_DEAL:
			return triggersFipeDeal(alert, item);
		default:
			return false;
		}
	}

	private boolean triggersClosingSoon(VehicleAlert alert, AuctionItem item, LocalDateTime now) {
		LocalDateTime closing = closingDate(item);
		if (closing == null || closing.isBefore(now)) {
			return false;
		}
		int lead = alert.getLeadTimeMinutes() != null ? alert.getLeadTimeMinutes() : DEFAULT_LEAD_MINUTES;
		return !now.isBefore(closing.minusMinutes(lead));
	}

	private boolean triggersSoldBelow(VehicleAlert alert, AuctionItem item, LocalDateTime now) {
		LocalDateTime closing = closingDate(item);
		return closing != null && closing.isBefore(now)
				&& item.getCurrentBidValue() != null && alert.getThresholdValue() != null
				&& item.getCurrentBidValue().compareTo(alert.getThresholdValue()) <= 0;
	}

	private boolean triggersFipeDeal(VehicleAlert alert, AuctionItem item) {
		if (item.getFipeValue() == null || item.getCurrentBidValue() == null
				|| item.getFipeValue().signum() <= 0) {
			return false;
		}
		int percent = alert.getFipePercent() != null ? alert.getFipePercent() : DEFAULT_FIPE_PERCENT;
		BigDecimal ceiling = item.getFipeValue().multiply(BigDecimal.valueOf(percent))
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		return item.getCurrentBidValue().compareTo(ceiling) <= 0;
	}

	private boolean matchesRadius(VehicleAlert alert, AuctionItem item) {
		if (alert.getRadiusKm() == null) {
			return true;
		}
		Auction auction = item.getAuction();
		if (auction == null) {
			return false;
		}
		Double distance = distanceService.distanceKm(auction.getCity(), auction.getStateCode());
		// Sem coordenadas ainda (enfileirado para geocodificar): nao dispara neste ciclo.
		return distance != null && distance <= alert.getRadiusKm();
	}

	// ---------------------------------- Mensagens ----------------------------------

	private String buildMessage(VehicleAlert alert, AuctionItem item) {
		Auction auction = item.getAuction();
		StringBuilder sb = new StringBuilder();
		sb.append(emoji(alert.getType())).append(" *").append(alert.getName()).append("*\n");
		sb.append(headline(alert, item)).append("\n\n");
		sb.append(describe(item)).append("\n");
		if (item.getCurrentBidValue() != null) {
			sb.append("Lance atual: ").append(money(item.getCurrentBidValue())).append("\n");
		}
		if (item.getFipeValue() != null && item.getFipeValue().signum() > 0) {
			sb.append("FIPE: ").append(money(item.getFipeValue())).append("\n");
		}
		if (auction != null) {
			if (notBlank(auction.getCity())) {
				sb.append("Local: ").append(auction.getCity());
				if (notBlank(auction.getStateCode())) {
					sb.append("/").append(auction.getStateCode());
				}
				Double distance = alert.getRadiusKm() != null
						? distanceService.distanceKm(auction.getCity(), auction.getStateCode())
						: null;
				if (distance != null) {
					sb.append(" (~").append(Math.round(distance)).append(" km)");
				}
				sb.append("\n");
			}
			LocalDateTime closing = auction.getClosingDate();
			if (closing != null) {
				sb.append("Encerra: ").append(DATE_TIME.format(closing)).append("\n");
			}
			if (notBlank(auction.getSourceUrl())) {
				sb.append("\n").append(auction.getSourceUrl());
			}
		}
		return sb.toString();
	}

	private String headline(VehicleAlert alert, AuctionItem item) {
		switch (alert.getType()) {
		case NEW_MATCH:
			return "Apareceu um veiculo que combina com o seu criterio.";
		case CLOSING_SOON:
			return "Este lote esta encerrando em breve!";
		case PRICE_ABOVE:
			return "O lance passou do seu teto de " + money(alert.getThresholdValue()) + ".";
		case SOLD_BELOW:
			return "Arrematado por " + money(item.getCurrentBidValue()) + ", abaixo do seu alvo de "
					+ money(alert.getThresholdValue()) + ".";
		case FIPE_DEAL:
			int percent = alert.getFipePercent() != null ? alert.getFipePercent() : DEFAULT_FIPE_PERCENT;
			return "Barganha: lance <= " + percent + "% da FIPE.";
		default:
			return "";
		}
	}

	private static String emoji(com.br.auction.garage.enums.AlertType type) {
		switch (type) {
		case CLOSING_SOON:
			return "⏰"; // relogio
		case PRICE_ABOVE:
			return "⚠️"; // aviso
		case SOLD_BELOW:
			return "🔨"; // martelo
		case FIPE_DEAL:
			return "💰"; // dinheiro
		case NEW_MATCH:
		default:
			return "🚗"; // carro
		}
	}

	private static String describe(AuctionItem item) {
		StringBuilder sb = new StringBuilder();
		if (notBlank(item.getLotNumber())) {
			sb.append("Lote ").append(item.getLotNumber()).append(" - ");
		}
		sb.append(notBlank(item.getVehicleDescription()) ? item.getVehicleDescription() : "Veiculo");
		return sb.toString();
	}

	private String recipient(VehicleAlert alert) {
		return notBlank(alert.getRecipientPhone()) ? alert.getRecipientPhone() : notifier.getDefaultRecipient();
	}

	private static LocalDateTime closingDate(AuctionItem item) {
		return item.getAuction() == null ? null : item.getAuction().getClosingDate();
	}

	private static String money(BigDecimal value) {
		return value == null ? "-" : BRL.format(value);
	}

	private static boolean notBlank(String value) {
		return value != null && !value.isBlank();
	}

	private void pace() {
		if (sendDelayMs <= 0) {
			return;
		}
		try {
			Thread.sleep(Duration.ofMillis(sendDelayMs).toMillis());
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}
}
