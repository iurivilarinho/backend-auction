package com.br.auction.garage.alert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.br.auction.garage.enums.AlertType;
import com.br.auction.garage.models.AlertNotification;
import com.br.auction.garage.models.VehicleAlert;
import com.br.auction.garage.repository.AlertNotificationRepository;
import com.br.auction.garage.repository.VehicleAlertRepository;
import com.br.auction.enums.AuctionStatus;
import com.br.auction.models.Auction;
import com.br.auction.models.AuctionItem;
import com.br.auction.notification.NotificationService;
import com.br.auction.notification.WhatsappNotifier;
import com.br.auction.notification.WhatsappNotifier.SendResult;
import com.br.auction.service.AuctionItemLinks;
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
	/** Ordem em que os gatilhos sao avaliados/enviados para um mesmo lote. */
	private static final AlertType[] TRIGGER_ORDER = { AlertType.NEW_MATCH, AlertType.OPENED, AlertType.PRICE_ABOVE,
			AlertType.FIPE_DEAL, AlertType.CLOSING_SOON, AlertType.SOLD_BELOW };

	private final VehicleAlertRepository alertRepository;
	private final VehicleAlertService alertService;
	private final AlertNotificationRepository notificationRepository;
	private final DistanceService distanceService;
	private final WhatsappNotifier notifier;
	private final NotificationService notificationService;
	private final int maxPerRun;
	private final long sendDelayMs;

	public AlertEvaluator(VehicleAlertRepository alertRepository, VehicleAlertService alertService,
			AlertNotificationRepository notificationRepository, DistanceService distanceService,
			WhatsappNotifier notifier, NotificationService notificationService,
			@Value("${whatsapp.alert.max-per-run:10}") int maxPerRun,
			@Value("${whatsapp.alert.send-delay-ms:2500}") long sendDelayMs) {
		this.alertRepository = alertRepository;
		this.alertService = alertService;
		this.notificationRepository = notificationRepository;
		this.distanceService = distanceService;
		this.notifier = notifier;
		this.notificationService = notificationService;
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
		List<String> recipients = notificationService.resolveRecipients(alert);
		if (recipients.isEmpty()) {
			LOG.debug("Alerta {} sem destinos habilitados; ignorado.", alert.getId());
			return;
		}
		int[] sent = { 0 };
		for (AuctionItem item : alertService.findCandidates(alert)) {
			if (!matchesRadius(alert, item)) {
				continue;
			}
			boolean withinLimit = true;
			// Um alerta pode ter varios gatilhos ligados: avalia cada um e dispara o que estiver ativo.
			for (AlertType trigger : TRIGGER_ORDER) {
				if (!enabled(alert, trigger) || !fires(alert, item, trigger, now)) {
					continue;
				}
				String key = trigger.name() + ":" + item.getId();
				withinLimit = dispatch(alert, item, key, buildMessage(alert, item, trigger, now), recipients, sent);
				if (!withinLimit) {
					break;
				}
			}
			if (!withinLimit) {
				LOG.info("Alerta {} atingiu o limite de {} envios nesta rodada; restante fica para a proxima.",
						alert.getId(), maxPerRun);
				break;
			}
		}
		if (sent[0] > 0) {
			LOG.info("Alerta {} ({}): {} envio(s) para {} destino(s).",
					alert.getId(), alert.getName(), sent[0], recipients.size());
		}
	}

	/**
	 * Envia uma notificacao (deduplicada por gatilho) para todos os destinos. Devolve {@code false}
	 * quando o limite por rodada foi atingido — sinal para a varredura parar este alerta.
	 */
	private boolean dispatch(VehicleAlert alert, AuctionItem item, String triggerKey, String message,
			List<String> recipients, int[] sent) {
		if (notificationRepository.existsByAlertIdAndTriggerKey(alert.getId(), triggerKey)) {
			return true;
		}
		if (sent[0] >= maxPerRun) {
			return false;
		}
		boolean anySent = false;
		String detail = null;
		for (String to : recipients) {
			SendResult result = notifier.send(to, message);
			detail = result.getDetail();
			if (result.isSent()) {
				anySent = true;
				sent[0]++;
				pace();
			} else {
				LOG.warn("Alerta {} item {} para {} nao enviado: {}", alert.getId(), item.getId(), to,
						result.getDetail());
			}
			if (sent[0] >= maxPerRun) {
				break;
			}
		}
		if (anySent) {
			// Dedup por gatilho (alerta+item): vale para todos os destinos desta rodada.
			notificationRepository.save(new AlertNotification(alert.getId(), item.getId(), triggerKey,
					SendResult.Status.SENT.name(), detail));
		}
		return sent[0] < maxPerRun;
	}

	/**
	 * Lotes que um alerta encontra agora (passam pelo criterio + raio + algum gatilho), sem enviar
	 * nada — usado pela tela de "Achados". Limitado para nao carregar listas enormes.
	 */
	public List<AuctionItem> findMatches(VehicleAlert alert, int limit) {
		LocalDateTime now = LocalDateTime.now();
		List<AuctionItem> out = new ArrayList<>();
		for (AuctionItem item : alertService.findCandidates(alert)) {
			if (matchesRadius(alert, item) && anyTriggerFires(alert, item, now)) {
				out.add(item);
				if (out.size() >= limit) {
					break;
				}
			}
		}
		return out;
	}

	// ---------------------------------- Gatilhos por tipo ----------------------------------

	private boolean anyTriggerFires(VehicleAlert alert, AuctionItem item, LocalDateTime now) {
		for (AlertType trigger : TRIGGER_ORDER) {
			if (enabled(alert, trigger) && fires(alert, item, trigger, now)) {
				return true;
			}
		}
		return false;
	}

	/** Se o gatilho esta habilitado no alerta. */
	private boolean enabled(VehicleAlert alert, AlertType trigger) {
		switch (trigger) {
		case NEW_MATCH:
			return Boolean.TRUE.equals(alert.getNotifyNewMatch());
		case OPENED:
			return Boolean.TRUE.equals(alert.getNotifyOnStart());
		case PRICE_ABOVE:
			return Boolean.TRUE.equals(alert.getNotifyPriceAbove());
		case FIPE_DEAL:
			return Boolean.TRUE.equals(alert.getNotifyFipeDeal());
		case CLOSING_SOON:
			return Boolean.TRUE.equals(alert.getNotifyClosingSoon());
		case SOLD_BELOW:
			return Boolean.TRUE.equals(alert.getNotifySoldBelow());
		default:
			return false;
		}
	}

	/** Se a condicao do gatilho esta satisfeita para o lote agora. */
	private boolean fires(VehicleAlert alert, AuctionItem item, AlertType trigger, LocalDateTime now) {
		switch (trigger) {
		case NEW_MATCH:
			return true;
		case OPENED:
			return item.getAuction() != null
					&& AuctionStatus.fromSource(item.getAuction().getStatus()) == AuctionStatus.EM_ANDAMENTO;
		case PRICE_ABOVE:
			return item.getCurrentBidValue() != null && alert.getThresholdValue() != null
					&& item.getCurrentBidValue().compareTo(alert.getThresholdValue()) > 0;
		case FIPE_DEAL:
			return triggersFipeDeal(alert, item);
		case CLOSING_SOON:
			return triggersClosingSoon(alert, item, now);
		case SOLD_BELOW:
			return triggersSoldBelow(alert, item, now);
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
				&& item.getCurrentBidValue() != null && alert.getSoldBelowValue() != null
				&& item.getCurrentBidValue().compareTo(alert.getSoldBelowValue()) <= 0;
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

	private String buildMessage(VehicleAlert alert, AuctionItem item, AlertType trigger, LocalDateTime now) {
		StringBuilder sb = new StringBuilder();
		sb.append(emoji(trigger)).append(" *").append(alert.getName()).append("*\n");
		sb.append(headline(alert, item, trigger, now)).append("\n\n");
		appendBody(sb, alert, item);
		return sb.toString();
	}

	/** Corpo comum: status (da pra dar lance?), veiculo, valores, local, encerramento e link direto. */
	private void appendBody(StringBuilder sb, VehicleAlert alert, AuctionItem item) {
		Auction auction = item.getAuction();
		String status = statusLine(auction);
		if (status != null) {
			sb.append(status).append("\n");
		}
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
			String lotUrl = AuctionItemLinks.lotUrl(item);
			if (notBlank(lotUrl)) {
				sb.append("\nVer o veiculo: ").append(lotUrl);
			}
		}
	}

	/** Linha de status: deixa claro se ja da pra dar lance. */
	private static String statusLine(Auction auction) {
		if (auction == null) {
			return null;
		}
		switch (AuctionStatus.fromSource(auction.getStatus())) {
		case EM_ANDAMENTO:
			return "🟢 Em andamento — ja da pra dar lance";
		case FINALIZADO:
			return "🔴 Encerrado — lances ja fecharam";
		case PUBLICADO:
		default:
			return "🟡 Publicado — lances ainda nao abriram";
		}
	}

	private static String closingHeadline(AuctionItem item, LocalDateTime now) {
		LocalDateTime closing = closingDate(item);
		if (closing == null || !closing.isAfter(now)) {
			return "Os lances estao encerrando!";
		}
		long minutes = Duration.between(now, closing).toMinutes();
		if (minutes >= 60) {
			long hours = minutes / 60;
			long rest = minutes % 60;
			String faltam = rest > 0 ? hours + "h" + String.format("%02d", rest) : hours + "h";
			return "Faltam ~" + faltam + " para encerrar os lances!";
		}
		return "Faltam ~" + Math.max(1, minutes) + " min para encerrar os lances!";
	}

	private String headline(VehicleAlert alert, AuctionItem item, AlertType trigger, LocalDateTime now) {
		switch (trigger) {
		case NEW_MATCH:
			return "Apareceu um veiculo que combina com o seu criterio.";
		case OPENED:
			return "Abriu pra lances! Ja da pra dar lance.";
		case PRICE_ABOVE:
			return "O lance passou do seu teto de " + money(alert.getThresholdValue()) + ".";
		case FIPE_DEAL:
			int percent = alert.getFipePercent() != null ? alert.getFipePercent() : DEFAULT_FIPE_PERCENT;
			return "Barganha: lance <= " + percent + "% da FIPE.";
		case CLOSING_SOON:
			return closingHeadline(item, now);
		case SOLD_BELOW:
			return "Arrematado por " + money(item.getCurrentBidValue()) + ", abaixo do seu alvo de "
					+ money(alert.getSoldBelowValue()) + ".";
		default:
			return "";
		}
	}

	private static String emoji(AlertType type) {
		switch (type) {
		case OPENED:
			return "🟢"; // verde (abriu)
		case PRICE_ABOVE:
			return "⚠️"; // aviso
		case FIPE_DEAL:
			return "💰"; // dinheiro
		case CLOSING_SOON:
			return "⏰"; // relogio
		case SOLD_BELOW:
			return "🔨"; // martelo
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
