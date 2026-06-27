package com.br.auction.garage.alert;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.br.auction.garage.enums.AlertType;
import com.br.auction.garage.models.VehicleAlert;
import com.br.auction.garage.repository.AlertNotificationRepository;
import com.br.auction.garage.repository.VehicleAlertRepository;
import com.br.auction.models.AuctionItem;
import com.br.auction.repository.AuctionItemRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class VehicleAlertService {

	private final VehicleAlertRepository repository;
	private final AuctionItemRepository auctionItemRepository;
	private final AlertNotificationRepository notificationRepository;

	public VehicleAlertService(VehicleAlertRepository repository, AuctionItemRepository auctionItemRepository,
			AlertNotificationRepository notificationRepository) {
		this.repository = repository;
		this.auctionItemRepository = auctionItemRepository;
		this.notificationRepository = notificationRepository;
	}

	public List<VehicleAlert> findAll() {
		return repository.findAllByOrderByCreatedAtDesc();
	}

	public VehicleAlert findById(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Alerta nao encontrado: " + id));
	}

	@Transactional
	public VehicleAlert create(VehicleAlertRequest request) {
		VehicleAlert alert = new VehicleAlert();
		apply(alert, request);
		return repository.save(alert);
	}

	@Transactional
	public VehicleAlert update(Long id, VehicleAlertRequest request) {
		VehicleAlert alert = findById(id);
		apply(alert, request);
		return repository.save(alert);
	}

	@Transactional
	public void delete(Long id) {
		VehicleAlert alert = findById(id);
		notificationRepository.deleteByAlertId(alert.getId());
		repository.delete(alert);
	}

	/**
	 * Conta quantos veiculos atualmente atendem ao criterio de selecao do alerta (ignora o raio,
	 * que depende de geocodificacao). Da uma ideia do alcance do alerta na tela.
	 */
	public int countMatches(VehicleAlert alert) {
		return (int) auctionItemRepository.count(AlertSpecifications.forAlert(alert));
	}

	List<AuctionItem> findCandidates(VehicleAlert alert) {
		return auctionItemRepository.findAll(AlertSpecifications.forAlert(alert));
	}

	private void apply(VehicleAlert alert, VehicleAlertRequest request) {
		alert.setName(request.getName());
		alert.setType(request.getType() == null ? AlertType.NEW_MATCH : request.getType());
		alert.setKeyword(blankToNull(request.getKeyword()));
		alert.setBrand(blankToNull(request.getBrand()));
		alert.setModel(blankToNull(request.getModel()));
		alert.setCity(blankToNull(request.getCity()));
		alert.setLotType(blankToNull(request.getLotType()));
		alert.setMaxBid(request.getMaxBid());
		alert.setRadiusKm(request.getRadiusKm());
		alert.setThresholdValue(request.getThresholdValue());
		alert.setFipePercent(request.getFipePercent());
		alert.setLeadTimeMinutes(request.getLeadTimeMinutes());
		alert.setRecipientPhone(blankToNull(request.getRecipientPhone()));
		alert.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
