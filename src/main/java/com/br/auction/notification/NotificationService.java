package com.br.auction.notification;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.br.auction.garage.models.VehicleAlert;

import jakarta.persistence.EntityNotFoundException;

/**
 * Regras de negocio do canal de notificacao: preferencias (liga/desliga), destinos (contatos e
 * grupos) e a resolucao de para quem cada alerta deve ser enviado.
 */
@Service
public class NotificationService {

	private final NotificationSettingsRepository settingsRepository;
	private final NotificationDestinationRepository destinationRepository;

	public NotificationService(NotificationSettingsRepository settingsRepository,
			NotificationDestinationRepository destinationRepository) {
		this.settingsRepository = settingsRepository;
		this.destinationRepository = destinationRepository;
	}

	// ----------------------------- Preferencias -----------------------------

	@Transactional
	public NotificationSettings getOrCreateSettings() {
		return settingsRepository.findById(NotificationSettings.SINGLETON_ID).orElseGet(() -> {
			NotificationSettings settings = new NotificationSettings();
			settings.setId(NotificationSettings.SINGLETON_ID);
			settings.setEnabled(Boolean.FALSE);
			return settingsRepository.save(settings);
		});
	}

	@Transactional
	public NotificationSettings updateEnabled(boolean enabled) {
		NotificationSettings settings = getOrCreateSettings();
		settings.setEnabled(enabled);
		return settingsRepository.save(settings);
	}

	// ----------------------------- Destinos -----------------------------

	public List<NotificationDestination> findDestinations() {
		return destinationRepository.findAllByOrderByCreatedAtAsc();
	}

	@Transactional
	public NotificationDestination createDestination(String label, DestinationType type, String value,
			Boolean enabled) {
		NotificationDestination destination = new NotificationDestination();
		applyDestination(destination, label, type, value, enabled);
		return destinationRepository.save(destination);
	}

	@Transactional
	public NotificationDestination updateDestination(Long id, String label, DestinationType type, String value,
			Boolean enabled) {
		NotificationDestination destination = destinationRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Destino nao encontrado: " + id));
		applyDestination(destination, label, type, value, enabled);
		return destinationRepository.save(destination);
	}

	@Transactional
	public void deleteDestination(Long id) {
		if (!destinationRepository.existsById(id)) {
			throw new EntityNotFoundException("Destino nao encontrado: " + id);
		}
		destinationRepository.deleteById(id);
	}

	private void applyDestination(NotificationDestination destination, String label, DestinationType type, String value,
			Boolean enabled) {
		DestinationType resolvedType = type == null ? DestinationType.CONTACT : type;
		destination.setLabel(label == null ? "" : label.trim());
		destination.setType(resolvedType);
		destination.setValue(normalizeValue(resolvedType, value));
		destination.setEnabled(enabled == null ? Boolean.TRUE : enabled);
	}

	/** Contato: so digitos (E.164 sem +). Grupo: mantem o JID (...@g.us). */
	private String normalizeValue(DestinationType type, String value) {
		String raw = value == null ? "" : value.trim();
		if (type == DestinationType.GROUP) {
			return raw;
		}
		return raw.replaceAll("[^0-9]", "");
	}

	/**
	 * Resolve os destinatarios de um alerta: o numero proprio do alerta tem prioridade; senao,
	 * todos os destinos habilitados.
	 */
	public List<String> resolveRecipients(VehicleAlert alert) {
		if (alert.getRecipientPhone() != null && !alert.getRecipientPhone().isBlank()) {
			return List.of(alert.getRecipientPhone().trim());
		}
		return destinationRepository.findByEnabledTrue().stream()
				.map(NotificationDestination::getValue)
				.filter(value -> value != null && !value.isBlank())
				.toList();
	}
}
