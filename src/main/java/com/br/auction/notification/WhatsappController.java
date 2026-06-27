package com.br.auction.notification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.auction.notification.WhatsappNotifier.SendResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Operacao do canal WhatsApp pela tela de Parametros: status/conexao, QR para parear, gestao de
 * destinos (contatos e grupos), envio de teste e webhook da Evolution.
 */
@RestController
@RequestMapping("/api/whatsapp")
@Tag(name = "WhatsApp", description = "Canal de notificacao via WhatsApp (Evolution API)")
public class WhatsappController {

	private static final Logger LOG = LoggerFactory.getLogger(WhatsappController.class);

	private final WhatsappNotifier notifier;
	private final NotificationService service;

	public WhatsappController(WhatsappNotifier notifier, NotificationService service) {
		this.notifier = notifier;
		this.service = service;
	}

	// ----------------------------- Status / conexao -----------------------------

	@Operation(summary = "Status do canal e da conexao")
	@GetMapping("/status")
	public ResponseEntity<Map<String, Object>> status() {
		Map<String, Object> out = new HashMap<>();
		out.put("enabled", notifier.isEnabled());
		out.put("hasApiKey", notifier.hasApiKey());
		out.put("configured", notifier.isConfigured());
		out.put("instance", notifier.getInstance());
		out.put("connectionState", notifier.connectionState());
		return ResponseEntity.ok(out);
	}

	@Operation(summary = "Ligar/desligar o canal")
	@PutMapping("/settings")
	public ResponseEntity<Map<String, Object>> updateSettings(@RequestBody SettingsRequest request) {
		boolean enabled = request != null && Boolean.TRUE.equals(request.getEnabled());
		service.updateEnabled(enabled);
		return ResponseEntity.ok(Map.of("enabled", enabled));
	}

	@Operation(summary = "QR / pareamento da instancia")
	@GetMapping("/qr")
	public ResponseEntity<Map<String, Object>> qr() {
		return ResponseEntity.ok(notifier.connect());
	}

	@Operation(summary = "Desconectar o numero (logout)")
	@PostMapping("/logout")
	public ResponseEntity<Map<String, Object>> logout() {
		boolean ok = notifier.logout();
		return ResponseEntity.ok(Map.of("success", ok));
	}

	@Operation(summary = "Listar grupos do numero logado")
	@GetMapping("/groups")
	public ResponseEntity<List<Map<String, Object>>> groups() {
		return ResponseEntity.ok(notifier.listGroups());
	}

	// ----------------------------- Destinos -----------------------------

	@Operation(summary = "Listar destinos")
	@GetMapping("/destinations")
	public ResponseEntity<List<DestinationResponse>> listDestinations() {
		return ResponseEntity.ok(service.findDestinations().stream().map(DestinationResponse::new).toList());
	}

	@Operation(summary = "Criar destino")
	@PostMapping("/destinations")
	public ResponseEntity<DestinationResponse> createDestination(@RequestBody DestinationRequest request) {
		NotificationDestination created = service.createDestination(request.getLabel(), request.getType(),
				request.getValue(), request.getEnabled());
		return ResponseEntity.status(HttpStatus.CREATED).body(new DestinationResponse(created));
	}

	@Operation(summary = "Atualizar destino")
	@PutMapping("/destinations/{id}")
	public ResponseEntity<DestinationResponse> updateDestination(@PathVariable Long id,
			@RequestBody DestinationRequest request) {
		NotificationDestination updated = service.updateDestination(id, request.getLabel(), request.getType(),
				request.getValue(), request.getEnabled());
		return ResponseEntity.ok(new DestinationResponse(updated));
	}

	@Operation(summary = "Remover destino")
	@DeleteMapping("/destinations/{id}")
	public ResponseEntity<Void> deleteDestination(@PathVariable Long id) {
		service.deleteDestination(id);
		return ResponseEntity.noContent().build();
	}

	// ----------------------------- Teste / webhook -----------------------------

	@Operation(summary = "Enviar mensagem de teste")
	@PostMapping("/test")
	public ResponseEntity<Map<String, Object>> test(@RequestBody(required = false) TestMessageRequest request) {
		String message = request != null && request.getMessage() != null && !request.getMessage().isBlank()
				? request.getMessage()
				: "Teste de notificacao do sistema de leiloes. Se voce recebeu isto, o canal esta funcionando.";
		String target = request == null ? null : request.getTo();
		if ((target == null || target.isBlank()) && request != null && request.getDestinationId() != null) {
			target = service.findDestinations().stream()
					.filter(d -> d.getId().equals(request.getDestinationId()))
					.map(NotificationDestination::getValue)
					.findFirst().orElse(null);
		}
		SendResult result = target != null && !target.isBlank() ? notifier.sendTest(target, message)
				: notifier.sendTest(message);
		return ResponseEntity.ok(Map.of("status", result.getStatus().name(), "detail", result.getDetail()));
	}

	@Operation(summary = "Webhook de eventos da Evolution API")
	@PostMapping("/webhook")
	public ResponseEntity<Void> webhook(@RequestBody(required = false) Map<String, Object> payload) {
		Object event = payload == null ? null : payload.get("event");
		LOG.info("Webhook WhatsApp recebido: event={}", event);
		return ResponseEntity.ok().build();
	}

	// ----------------------------- DTOs -----------------------------

	public static class SettingsRequest {
		private Boolean enabled;

		public Boolean getEnabled() {
			return enabled;
		}

		public void setEnabled(Boolean enabled) {
			this.enabled = enabled;
		}
	}

	public static class DestinationRequest {
		private String label;
		private DestinationType type;
		private String value;
		private Boolean enabled;

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
	}

	public static class DestinationResponse {
		private final Long id;
		private final String label;
		private final DestinationType type;
		private final String typeLabel;
		private final String value;
		private final boolean enabled;

		public DestinationResponse(NotificationDestination destination) {
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

	public static class TestMessageRequest {
		private String to;
		private Long destinationId;
		private String message;

		public String getTo() {
			return to;
		}

		public void setTo(String to) {
			this.to = to;
		}

		public Long getDestinationId() {
			return destinationId;
		}

		public void setDestinationId(Long destinationId) {
			this.destinationId = destinationId;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}
}
