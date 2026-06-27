package com.br.auction.notification;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Canal de envio de mensagens via WhatsApp usando a Evolution API (gateway nao oficial, self-host).
 * O backend nunca fala com o WhatsApp direto: monta o texto e entrega para a Evolution, que mantem
 * a sessao (Baileys) e faz a entrega.
 *
 * <p>Infra (base-url, api-key, instancia) vem de variavel de ambiente. As preferencias editaveis na
 * tela (ligar/desligar e numero de destino) ficam em {@link NotificationSettings} no banco e tem
 * prioridade sobre os defaults de ambiente — assim o usuario muda em runtime sem redeploy.
 */
@Service
public class WhatsappNotifier {

	private static final Logger LOG = LoggerFactory.getLogger(WhatsappNotifier.class);

	private final boolean envEnabled;
	private final String baseUrl;
	private final String apiKey;
	private final String instance;
	private final String envRecipient;
	private final RestClient restClient;
	private final NotificationSettingsRepository settingsRepository;

	public WhatsappNotifier(
			@Value("${whatsapp.enabled:false}") boolean envEnabled,
			@Value("${whatsapp.evolution.base-url:http://localhost:8091}") String baseUrl,
			@Value("${whatsapp.evolution.api-key:}") String apiKey,
			@Value("${whatsapp.evolution.instance:leiloes}") String instance,
			@Value("${whatsapp.recipient:}") String envRecipient,
			NotificationSettingsRepository settingsRepository) {
		this.envEnabled = envEnabled;
		this.baseUrl = stripTrailingSlash(baseUrl);
		this.apiKey = apiKey;
		this.instance = instance;
		this.envRecipient = normalizeNumber(envRecipient);
		this.settingsRepository = settingsRepository;
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
		factory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
		this.restClient = RestClient.builder().requestFactory(factory).build();
	}

	// ---------------------------------- Preferencias (DB > env) ----------------------------------

	/** Canal ligado: usa o valor salvo no banco; cai no default de ambiente quando ainda nao ha linha. */
	@Transactional(readOnly = true)
	public boolean isEnabled() {
		NotificationSettings settings = settingsRepository.findById(NotificationSettings.SINGLETON_ID).orElse(null);
		return settings != null && settings.getEnabled() != null ? settings.getEnabled() : envEnabled;
	}

	/** API key presente (configuracao minima de infra para falar com a Evolution). */
	public boolean hasApiKey() {
		return !apiKey.isBlank();
	}

	/** Pronto para enviar: ligado e com api-key. */
	public boolean isConfigured() {
		return isEnabled() && hasApiKey();
	}

	public String getInstance() {
		return instance;
	}

	@Transactional(readOnly = true)
	public String getDefaultRecipient() {
		NotificationSettings settings = settingsRepository.findById(NotificationSettings.SINGLETON_ID).orElse(null);
		String recipient = settings != null ? settings.getRecipient() : null;
		return normalizeNumber(recipient != null && !recipient.isBlank() ? recipient : envRecipient);
	}

	// ---------------------------------- Envio ----------------------------------

	/** Envia para o destinatario global configurado (respeita o liga/desliga do canal). */
	public SendResult send(String text) {
		return send(getDefaultRecipient(), text);
	}

	/**
	 * Envia uma mensagem de texto (uso dos alertas automaticos). Respeita o liga/desliga do canal.
	 * Numero no formato internacional sem o + (ex.: 5534999998888) ou JID de grupo (...@g.us).
	 * Nunca lanca: devolve um {@link SendResult} para que a avaliacao em lote siga.
	 */
	public SendResult send(String number, String text) {
		return dispatch(number, text, true, false);
	}

	/** Envio de teste manual: ignora o liga/desliga, mas confirma que o numero esta conectado. */
	public SendResult sendTest(String text) {
		return sendTest(getDefaultRecipient(), text);
	}

	public SendResult sendTest(String number, String text) {
		return dispatch(number, text, false, true);
	}

	private SendResult dispatch(String number, String text, boolean requireEnabled, boolean checkConnection) {
		String to = normalizeRecipient(number);
		if (requireEnabled && !isEnabled()) {
			return SendResult.skipped("Canal desativado. Ative em Parametros > WhatsApp.");
		}
		if (apiKey.isBlank()) {
			return SendResult.skipped("Evolution sem API key — verifique a configuracao do servidor.");
		}
		if (to.isBlank()) {
			return SendResult.skipped("Informe um destino (nenhum numero/grupo definido).");
		}
		if (checkConnection && !"open".equals(connectionState())) {
			return SendResult.skipped("WhatsApp nao esta conectado. Escaneie o QR em Parametros > WhatsApp.");
		}
		try {
			restClient.post()
					.uri(baseUrl + "/message/sendText/" + instance)
					.header("apikey", apiKey)
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of("number", to, "text", text))
					.retrieve()
					.toBodilessEntity();
			LOG.info("WhatsApp enviado para {} ({} chars).", to, text.length());
			return SendResult.ok(to);
		} catch (RestClientResponseException ex) {
			String friendly = translateError(ex, to);
			LOG.warn("WhatsApp recusou envio para {}: {}", to, ex.getMessage());
			return SendResult.failed(friendly);
		} catch (RuntimeException ex) {
			LOG.warn("Falha ao enviar WhatsApp para {}: {}", to, ex.getMessage());
			return SendResult.failed("Nao foi possivel falar com o WhatsApp: " + ex.getMessage());
		}
	}

	/** Traduz os erros mais comuns da Evolution para algo acionavel pelo usuario. */
	private static String translateError(RestClientResponseException ex, String to) {
		String body = ex.getResponseBodyAsString();
		if (body != null && body.contains("\"exists\":false")) {
			return "O numero " + to + " nao tem WhatsApp. Confira o DDI/DDD e o 9o digito do celular.";
		}
		if (body != null && (body.contains("not found") || body.contains("Connection Closed")
				|| body.contains("connecting"))) {
			return "WhatsApp nao esta conectado. Escaneie o QR em Parametros > WhatsApp.";
		}
		return "WhatsApp recusou o envio (HTTP " + ex.getStatusCode().value() + ").";
	}

	/** Mantem o JID de grupo intacto; nos demais casos reduz a digitos (E.164 sem +). */
	private static String normalizeRecipient(String number) {
		if (number == null) {
			return "";
		}
		if (number.contains("@")) {
			return number.trim();
		}
		return normalizeNumber(number);
	}

	// ---------------------------------- Consulta a Evolution (status/QR) ----------------------------------

	/** Estado da conexao da instancia: "open" (logada), "connecting", "close"... ou null se indisponivel. */
	public String connectionState() {
		if (apiKey.isBlank()) {
			return null;
		}
		try {
			Map<?, ?> body = restClient.get()
					.uri(baseUrl + "/instance/connectionState/" + instance)
					.header("apikey", apiKey)
					.retrieve()
					.body(Map.class);
			Object inst = body == null ? null : body.get("instance");
			if (inst instanceof Map<?, ?> instMap) {
				Object state = instMap.get("state");
				return state == null ? null : state.toString();
			}
			return null;
		} catch (RuntimeException ex) {
			LOG.warn("Falha ao consultar estado da instancia {}: {}", instance, ex.getMessage());
			return null;
		}
	}

	/**
	 * Garante que a instancia exista e devolve o QR para parear (base64 + pairingCode) e o estado.
	 * Quando ja esta logada, a Evolution responde sem QR e o estado vem "open".
	 */
	public Map<String, Object> connect() {
		if (apiKey.isBlank()) {
			return Map.of("error", "API key da Evolution nao configurada.");
		}
		ensureInstance();
		try {
			Map<?, ?> body = restClient.get()
					.uri(baseUrl + "/instance/connect/" + instance)
					.header("apikey", apiKey)
					.retrieve()
					.body(Map.class);
			if (body == null) {
				return Map.of();
			}
			HashMap<String, Object> out = new HashMap<>();
			out.put("base64", body.get("base64"));
			out.put("pairingCode", body.get("pairingCode"));
			out.put("code", body.get("code"));
			out.put("state", connectionState());
			return out;
		} catch (RuntimeException ex) {
			LOG.warn("Falha ao obter QR da instancia {}: {}", instance, ex.getMessage());
			return Map.of("error", ex.getMessage());
		}
	}

	/** Desconecta (logout) a instancia para parear outro numero. */
	public boolean logout() {
		if (apiKey.isBlank()) {
			return false;
		}
		try {
			restClient.delete()
					.uri(baseUrl + "/instance/logout/" + instance)
					.header("apikey", apiKey)
					.retrieve()
					.toBodilessEntity();
			return true;
		} catch (RuntimeException ex) {
			LOG.warn("Falha ao desconectar instancia {}: {}", instance, ex.getMessage());
			return false;
		}
	}

	/** Lista os grupos em que o numero logado participa: [{ id (JID), subject }]. */
	public List<Map<String, Object>> listGroups() {
		if (apiKey.isBlank()) {
			return List.of();
		}
		try {
			Object body = restClient.get()
					.uri(baseUrl + "/group/fetchAllGroups/" + instance + "?getParticipants=false")
					.header("apikey", apiKey)
					.retrieve()
					.body(Object.class);
			List<Map<String, Object>> groups = new ArrayList<>();
			if (body instanceof List<?> list) {
				for (Object item : list) {
					if (item instanceof Map<?, ?> map) {
						Object subject = map.get("subject");
						groups.add(Map.of(
								"id", String.valueOf(map.get("id")),
								"subject", subject == null ? "" : subject.toString()));
					}
				}
			}
			return groups;
		} catch (RuntimeException ex) {
			LOG.warn("Falha ao listar grupos da instancia {}: {}", instance, ex.getMessage());
			return java.util.List.of();
		}
	}

	/** Cria a instancia se ainda nao existir (idempotente). */
	private void ensureInstance() {
		try {
			restClient.post()
					.uri(baseUrl + "/instance/create")
					.header("apikey", apiKey)
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of("instanceName", instance, "integration", "WHATSAPP-BAILEYS", "qrcode", true))
					.retrieve()
					.toBodilessEntity();
		} catch (RuntimeException ex) {
			// Ja existe (Evolution responde 403/409): segue para o connect normalmente.
			LOG.debug("Instancia {} ja existe ou create ignorado: {}", instance, ex.getMessage());
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
