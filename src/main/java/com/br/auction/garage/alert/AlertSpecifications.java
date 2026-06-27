package com.br.auction.garage.alert;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.br.auction.garage.models.VehicleAlert;
import com.br.auction.models.AuctionItem;

import jakarta.persistence.criteria.Predicate;

/**
 * Constroi o filtro SQL que seleciona os lotes candidatos de um alerta a partir dos seus criterios
 * (palavra-chave, marca, modelo, cidade, tipo, lance maximo). O raio em km nao entra aqui — ele
 * depende de geocodificacao e e aplicado em memoria pelo avaliador. Compartilhado entre o service
 * (contagem de correspondencias) e o avaliador (disparo).
 */
final class AlertSpecifications {

	private AlertSpecifications() {
	}

	static Specification<AuctionItem> forAlert(VehicleAlert alert) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (notBlank(alert.getKeyword())) {
				// Busca por trecho ignorando espacos: "xr 200" casa com "XR200" e "XR 200".
				jakarta.persistence.criteria.Expression<String> descNoSpace = cb.function("replace", String.class,
						cb.lower(root.get("vehicleDescription")), cb.literal(" "), cb.literal(""));
				String keyword = alert.getKeyword().toLowerCase().replace(" ", "");
				predicates.add(cb.like(descNoSpace, "%" + keyword + "%"));
			}
			if (alert.getMinYear() != null) {
				// vehicleYear e texto ("2012" ou "2012/2013"); comparacao lexical funciona p/ anos de 4 digitos.
				predicates.add(cb.greaterThanOrEqualTo(root.get("vehicleYear"), alert.getMinYear().toString()));
			}
			if (notBlank(alert.getBrand())) {
				predicates.add(cb.like(cb.lower(root.get("brand")), "%" + alert.getBrand().toLowerCase() + "%"));
			}
			if (notBlank(alert.getModel())) {
				predicates.add(cb.like(cb.lower(root.get("model")), "%" + alert.getModel().toLowerCase() + "%"));
			}
			if (notBlank(alert.getLotType())) {
				predicates.add(cb.equal(cb.lower(root.get("lotType")), alert.getLotType().toLowerCase()));
			}
			if (alert.getMaxBid() != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("currentBidValue"), alert.getMaxBid()));
			}
			if (notBlank(alert.getCity())) {
				predicates.add(cb.like(cb.lower(root.join("auction").get("city")),
						"%" + alert.getCity().toLowerCase() + "%"));
			}
			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	private static boolean notBlank(String value) {
		return value != null && !value.isBlank();
	}
}
