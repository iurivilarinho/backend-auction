package com.br.auction.garage.alert;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.br.auction.garage.models.VehicleAlert;
import com.br.auction.garage.repository.VehicleAlertRepository;
import com.br.auction.models.AuctionItem;
import com.br.auction.repository.AuctionItemRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class VehicleAlertService {

	private final VehicleAlertRepository repository;
	private final AuctionItemRepository auctionItemRepository;

	public VehicleAlertService(VehicleAlertRepository repository, AuctionItemRepository auctionItemRepository) {
		this.repository = repository;
		this.auctionItemRepository = auctionItemRepository;
	}

	public java.util.List<VehicleAlert> findAll() {
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
		repository.delete(findById(id));
	}

	/**
	 * Conta quantos veiculos atualmente atendem ao criterio do alerta.
	 */
	public int countMatches(VehicleAlert alert) {
		return (int) auctionItemRepository.count(buildSpecification(alert));
	}

	private void apply(VehicleAlert alert, VehicleAlertRequest request) {
		alert.setName(request.getName());
		alert.setKeyword(blankToNull(request.getKeyword()));
		alert.setCity(blankToNull(request.getCity()));
		alert.setLotType(blankToNull(request.getLotType()));
		alert.setMaxBid(request.getMaxBid());
		alert.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
	}

	private Specification<AuctionItem> buildSpecification(VehicleAlert alert) {
		return (root, query, cb) -> {
			java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
			if (alert.getKeyword() != null) {
				predicates.add(cb.like(cb.lower(root.get("vehicleDescription")),
						"%" + alert.getKeyword().toLowerCase() + "%"));
			}
			if (alert.getLotType() != null) {
				predicates.add(cb.equal(cb.lower(root.get("lotType")), alert.getLotType().toLowerCase()));
			}
			if (alert.getMaxBid() != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("currentBidValue"), alert.getMaxBid()));
			}
			if (alert.getCity() != null) {
				predicates.add(cb.like(cb.lower(root.join("auction").get("city")),
						"%" + alert.getCity().toLowerCase() + "%"));
			}
			return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
		};
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
