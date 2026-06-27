package com.br.auction.garage.alert;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.br.auction.garage.models.VehicleAlert;
import com.br.auction.garage.repository.VehicleAlertRepository;
import com.br.auction.models.AuctionItem;
import com.br.auction.service.DistanceService;

/**
 * Monta os "Achados" para a tela: para cada alerta, os veiculos que ele encontra agora (sem enviar
 * nada), com a distancia calculada e a marca de "ja notificado".
 */
@Service
public class AlertFindingService {

	private static final int MAX_PER_ALERT = 100;

	private final VehicleAlertRepository alertRepository;
	private final VehicleAlertService alertService;
	private final AlertEvaluator evaluator;
	private final DistanceService distanceService;

	public AlertFindingService(VehicleAlertRepository alertRepository, VehicleAlertService alertService,
			AlertEvaluator evaluator, DistanceService distanceService) {
		this.alertRepository = alertRepository;
		this.alertService = alertService;
		this.evaluator = evaluator;
		this.distanceService = distanceService;
	}

	@Transactional(readOnly = true)
	public List<AlertFindingResponse> findAll() {
		List<AlertFindingResponse> findings = new ArrayList<>();
		for (VehicleAlert alert : alertRepository.findAllByOrderByCreatedAtDesc()) {
			for (AuctionItem item : evaluator.findMatches(alert, MAX_PER_ALERT)) {
				Double distance = item.getAuction() == null ? null
						: distanceService.distanceKm(item.getAuction().getCity(), item.getAuction().getStateCode());
				findings.add(new AlertFindingResponse(alert, item, alertService.wasNotified(alert, item.getId()),
						distance));
			}
		}
		return findings;
	}

	@Transactional(readOnly = true)
	public List<AlertFindingResponse> findByAlert(Long alertId) {
		VehicleAlert alert = alertService.findById(alertId);
		List<AlertFindingResponse> findings = new ArrayList<>();
		for (AuctionItem item : evaluator.findMatches(alert, MAX_PER_ALERT)) {
			Double distance = item.getAuction() == null ? null
					: distanceService.distanceKm(item.getAuction().getCity(), item.getAuction().getStateCode());
			findings.add(new AlertFindingResponse(alert, item, alertService.wasNotified(alert, item.getId()),
					distance));
		}
		return findings;
	}
}
