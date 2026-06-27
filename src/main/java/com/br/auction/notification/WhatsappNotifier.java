package com.br.auction.notification;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Canal de envio de mensagens via WhatsApp usando a Evolution API (gateway nao oficial, self-host).
 * O backend nunca fala com o WhatsApp direto: monta o texto e entrega para a Evolution, que mantem
 * a sessao (Baileys) e faz a entrega. Mantido isolado para que o resto da aplicacao so dependa de
 * {@link #send(String, String)}.
 */
@Service
public class WhatsappNotifier {

	private static final Logger LOG = LoggerFactory.getLogger(WhatsappNotifier.class);

	private final boolean enabled;
	private final String baseUrl;
	private final String apiKey;
	private final String instance;
	private final String defaultRecipient;
	private final RestClient restClient;

	public WhatsappNotifier(
			@Value("${whatsapp.enabled:false}") boolean enabled,
			@Value("${whatsapp.evolution.base-url:http://localhost:8090}") String baseUrl,
			@Value("${whatsapp.evolution.api-key:}") String apiKey,
			@Value("${whatsapp.evolution.instance:leiloes}") String instance,
			@Value("${whatsapp.recipient:}") String defaultRecipient) {
		this.enabled = enabled;
		this.baseUrl = stripTrailingSlash(baseUrl);
		this.apiKey = apiKey;
		this.instance = instance;
		this.defaultRecipient = normalizeNumber(defaultRecipient);
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
		factory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
		this.restClient = RestClient.builder().requestFactory(factory).build();
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isConfigured() {
		return enabled && !apiKey.isBlank();
	}

	public String getInstance() {
		return instance;
	}

	public String getDefaultRecipient() {
		return defaultRecipient;
	}

	/** Envia para o destinatario global configurado em {@code whatsapp.recipient}. */
	public SendResult send(String text) {
		return send(defaultRecipient, text);
	}

	/**
	 * Envia uma mensagem de texto. Numero no formato internacional sem o sinal de +
	 * (ex.: 5534999998888). Nunca lanca: devolve um {@link SendResult} para que a avaliacao
	 * de alertas siga mesmo quando o canal esta indisponivel.
	 */
	public SendResult send(String number, String text) {
		String to = normalizeNumber(number);
		if (!enabled) {
			return SendResult.skipped("Canal WhatsApp desativado (whatsapp.enabled=false).");
		}
		if (apiKey.isBlank()) {
			return SendResult.skipped("API key da Evolution nao configurada (whatsapp.evolution.api-key).");
		}
		if (to.isBlank()) {
			return SendResult.skipped("Destinatario nao informado e whatsapp.recipient vazio.");
		}
		String url = baseUrl + "/message/sendText/" + instance;
		try {
			restClient.post()
					.uri(url)
					.header("apikey", apiKey)
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of("number", to, "text", text))
					.retrieve()
					.toBodilessEntity();
			LOG.info("WhatsApp enviado para {} ({} chars).", to, text.length());
			return SendResult.ok(to);
		} catch (RuntimeException ex) {
			LOG.warn("Falha ao enviar WhatsApp para {}: {}", to, ex.getMessage());
			return SendResult.failed(ex.getMessage());
		}
	}

	private static String normalizeNumber(String number) {
		if (number == null) {
			return "";
		}
		return number.replaceAll("[^0-9]", "");
	}

	private static String stripTrailingSlash(String value) {
		if (value == null || value.isBlank()) {
			return "";
		}
		return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
	}

	/** Resultado do envio: nao usa excecao para nao interromper a avaliacao em lote de alertas. */
	public static final class SendResult {

		public enum Status {
			SENT, SKIPPED, FAILED
		}

		private final Status status;
		private final String detail;

		private SendResult(Status status, String detail) {
			this.status = status;
			this.detail = detail;
		}

		static SendResult ok(String number) {
			return new SendResult(Status.SENT, "Enviado para " + number);
		}

		static SendResult skipped(String reason) {
			return new SendResult(Status.SKIPPED, reason);
		}

		static SendResult failed(String reason) {
			return new SendResult(Status.FAILED, reason);
		}

		public boolean isSent() {
			return status == Status.SENT;
		}

		public Status getStatus() {
			return status;
		}

		public String getDetail() {
			return detail;
		}
	}
}
