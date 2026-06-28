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

	@Transactional(readOnly = true)
	public List<VehicleAlert> findAll() {
		return repository.findAllByOrderByCreatedAtDesc();
	}

	@Transactional(readOnly = true)
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
	@Transactional(readOnly = true)
	public int countMatches(VehicleAlert alert) {
		return (int) auctionItemRepository.count(AlertSpecifications.forAlert(alert));
	}

	@Transactional(readOnly = true)
	public List<AuctionItem> findCandidates(VehicleAlert alert) {
		return auctionItemRepository.findAll(AlertSpecifications.forAlert(alert));
	}

	@Transactional(readOnly = true)
	public boolean wasNotified(VehicleAlert alert, Long auctionItemId) {
		// Multi-gatilho: considera "ja avisado" se qualquer gatilho do alerta ja notificou este lote.
		return notificationRepository.existsByAlertIdAndAuctionItemId(alert.getId(), auctionItemId);
	}

	private void apply(VehicleAlert alert, VehicleAlertRequest request) {
		alert.setName(request.getName());
		alert.setKeyword(blankToNull(request.getKeyword()));
		alert.setBrand(blankToNull(request.getBrand()));
		alert.setModel(blankToNull(request.getModel()));
		alert.setCity(blankToNull(request.getCity()));
		alert.setLotType(blankToNull(request.getLotType()));
		alert.setMaxBid(request.getMaxBid());
		alert.setRadiusKm(request.getRadiusKm());
		alert.setMinYear(request.getMinYear());
		alert.setThresholdValue(request.getThresholdValue());
		alert.setSoldBelowValue(request.getSoldBelowValue());
		alert.setFipePercent(request.getFipePercent());
		alert.setLeadTimeMinutes(request.getLeadTimeMinutes());

		boolean newMatch = Boolean.TRUE.equals(request.getNotifyNewMatch());
		boolean onStart = Boolean.TRUE.equals(request.getNotifyOnStart());
		boolean priceAbove = Boolean.TRUE.equals(request.getNotifyPriceAbove());
		boolean fipeDeal = Boolean.TRUE.equals(request.getNotifyFipeDeal());
		boolean closingSoon = Boolean.TRUE.equals(request.getNotifyClosingSoon());
		boolean noBidsClosing = Boolean.TRUE.equals(request.getNotifyNoBidsClosing());
		boolean soldBelow = Boolean.TRUE.equals(request.getNotifySoldBelow());
		// Um alerta sem nenhum gatilho nao avisaria nada: cai no padrao "novo correspondente".
		if (!(newMatch || onStart || priceAbove || fipeDeal || closingSoon || noBidsClosing || soldBelow)) {
			newMatch = true;
		}
		alert.setNotifyNewMatch(newMatch);
		alert.setNotifyOnStart(onStart);
		alert.setNotifyPriceAbove(priceAbove);
		alert.setNotifyFipeDeal(fipeDeal);
		alert.setNotifyClosingSoon(closingSoon);
		alert.setNotifyNoBidsClosing(noBidsClosing);
		alert.setNotifySoldBelow(soldBelow);
		// `type` segue preenchido (gatilho representativo) para exibicao e por ser NOT NULL no banco.
		alert.setType(representativeType(alert));

		alert.setRecipientPhone(blankToNull(request.getRecipientPhone()));
		alert.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
	}

	/** Primeiro gatilho habilitado (ordem de fluxo) — usado so para exibicao/retrocompatibilidade. */
	private AlertType representativeType(VehicleAlert alert) {
		if (Boolean.TRUE.equals(alert.getNotifyNewMatch())) {
			return AlertType.NEW_MATCH;
		}
		if (Boolean.TRUE.equals(alert.getNotifyOnStart())) {
			return AlertType.OPENED;
		}
		if (Boolean.TRUE.equals(alert.getNotifyPriceAbove())) {
			return AlertType.PRICE_ABOVE;
		}
		if (Boolean.TRUE.equals(alert.getNotifyFipeDeal())) {
			return AlertType.FIPE_DEAL;
		}
		if (Boolean.TRUE.equals(alert.getNotifyClosingSoon())) {
			return AlertType.CLOSING_SOON;
		}
		if (Boolean.TRUE.equals(alert.getNotifyNoBidsClosing())) {
			return AlertType.NO_BIDS_CLOSING;
		}
		if (Boolean.TRUE.equals(alert.getNotifySoldBelow())) {
			return AlertType.SOLD_BELOW;
		}
		return AlertType.NEW_MATCH;
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
