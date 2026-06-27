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
import jakarta.validation.Valid;

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
	public ResponseEntity<Map<String, Object>> updateSettings(@RequestBody WhatsappSettingsRequest request) {
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
		return ResponseEntity.ok(Map.of("success", notifier.logout()));
	}

	@Operation(summary = "Listar grupos do numero logado")
	@GetMapping("/groups")
	public ResponseEntity<List<Map<String, Object>>> groups() {
		return ResponseEntity.ok(notifier.listGroups());
	}

	// ----------------------------- Destinos -----------------------------

	@Operation(summary = "Listar destinos")
	@GetMapping("/destinations")
	public ResponseEntity<List<NotificationDestinationResponse>> listDestinations() {
		return ResponseEntity.ok(service.findDestinations().stream().map(NotificationDestinationResponse::new).toList());
	}

	@Operation(summary = "Criar destino")
	@PostMapping("/destinations")
	public ResponseEntity<NotificationDestinationResponse> createDestination(
			@Valid @RequestBody NotificationDestinationRequest request) {
		NotificationDestination created = service.createDestination(request.getLabel(), request.getType(),
				request.getValue(), request.getEnabled());
		return ResponseEntity.status(HttpStatus.CREATED).body(new NotificationDestinationResponse(created));
	}

	@Operation(summary = "Atualizar destino")
	@PutMapping("/destinations/{id}")
	public ResponseEntity<NotificationDestinationResponse> updateDestination(@PathVariable Long id,
			@Valid @RequestBody NotificationDestinationRequest request) {
		NotificationDestination updated = service.updateDestination(id, request.getLabel(), request.getType(),
				request.getValue(), request.getEnabled());
		return ResponseEntity.ok(new NotificationDestinationResponse(updated));
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
	public ResponseEntity<Map<String, Object>> test(@RequestBody(required = false) WhatsappTestRequest request) {
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
}
