package com.br.auction.notification;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.auction.notification.WhatsappNotifier.SendResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Endpoints de operacao do canal WhatsApp: status, envio de teste e recebimento de webhooks da
 * Evolution API (eventos de conexao/entrega). O envio real de alertas e feito pelo agendador.
 */
@RestController
@RequestMapping("/api/whatsapp")
@Tag(name = "WhatsApp", description = "Canal de notificacao via WhatsApp (Evolution API)")
public class WhatsappController {

	private static final Logger LOG = LoggerFactory.getLogger(WhatsappController.class);

	private final WhatsappNotifier notifier;

	public WhatsappController(WhatsappNotifier notifier) {
		this.notifier = notifier;
	}

	@Operation(summary = "Status do canal WhatsApp")
	@GetMapping("/status")
	public ResponseEntity<Map<String, Object>> status() {
		return ResponseEntity.ok(Map.of(
				"enabled", notifier.isEnabled(),
				"configured", notifier.isConfigured(),
				"instance", notifier.getInstance(),
				"recipient", maskNumber(notifier.getDefaultRecipient())));
	}

	@Operation(summary = "Enviar mensagem de teste")
	@PostMapping("/test")
	public ResponseEntity<Map<String, Object>> test(@RequestBody(required = false) TestMessageRequest request) {
		String message = request != null && request.getMessage() != null && !request.getMessage().isBlank()
				? request.getMessage()
				: "Teste de notificacao do sistema de leiloes. Se voce recebeu isto, o canal esta funcionando.";
		SendResult result = request != null && request.getTo() != null && !request.getTo().isBlank()
				? notifier.send(request.getTo(), message)
				: notifier.send(message);
		return ResponseEntity.ok(Map.of(
				"status", result.getStatus().name(),
				"detail", result.getDetail()));
	}

	@Operation(summary = "Webhook de eventos da Evolution API")
	@PostMapping("/webhook")
	public ResponseEntity<Void> webhook(@RequestBody(required = false) Map<String, Object> payload) {
		Object event = payload == null ? null : payload.get("event");
		LOG.info("Webhook WhatsApp recebido: event={}", event);
		return ResponseEntity.ok().build();
	}

	private static String maskNumber(String number) {
		if (number == null || number.length() < 4) {
			return "";
		}
		return "****" + number.substring(number.length() - 4);
	}

	public static class TestMessageRequest {
		private String to;
		private String message;

		public String getTo() {
			return to;
		}

		public void setTo(String to) {
			this.to = to;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}
}
