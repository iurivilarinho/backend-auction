package com.br.auction.garage.alert;

import java.math.BigDecimal;

import org.springframework.data.jpa.domain.Specification;

import com.br.auction.garage.models.VehicleAlert;
import com.br.auction.models.AuctionItem;

/**
 * Constroi o filtro SQL que seleciona os lotes candidatos de um alerta a partir dos seus criterios.
 * Cada criterio fica isolado num metodo que se anula (conjuncao) quando o valor e nulo/vazio, no
 * mesmo padrao das demais Specifications do projeto. O raio em km nao entra aqui — depende de
 * geocodificacao e e aplicado em memoria pelo avaliador.
 */
final class AlertSpecifications {

	private AlertSpecifications() {
	}

	static Specification<AuctionItem> forAlert(VehicleAlert alert) {
		return Specification.allOf(
				keyword(alert.getKeyword()),
				brand(alert.getBrand()),
				model(alert.getModel()),
				lotType(alert.getLotType()),
				maxBid(alert.getMaxBid()),
				minYear(alert.getMinYear()),
				city(alert.getCity()));
	}

	/** Busca por trecho ignorando espacos: "xr 200" casa com "XR200" e "XR 200". */
	private static Specification<AuctionItem> keyword(String keyword) {
		if (isBlank(keyword)) {
			return noop();
		}
		String normalized = keyword.toLowerCase().replace(" ", "");
		return (root, query, cb) -> cb.like(
				cb.function("replace", String.class, cb.lower(root.get("vehicleDescription")), cb.literal(" "),
						cb.literal("")),
				"%" + normalized + "%");
	}

	private static Specification<AuctionItem> brand(String brand) {
		return isBlank(brand) ? noop()
				: (root, query, cb) -> cb.like(cb.lower(root.get("brand")), "%" + brand.toLowerCase() + "%");
	}

	private static Specification<AuctionItem> model(String model) {
		return isBlank(model) ? noop()
				: (root, query, cb) -> cb.like(cb.lower(root.get("model")), "%" + model.toLowerCase() + "%");
	}

	private static Specification<AuctionItem> lotType(String lotType) {
		return isBlank(lotType) ? noop()
				: (root, query, cb) -> cb.equal(cb.lower(root.get("lotType")), lotType.toLowerCase());
	}

	private static Specification<AuctionItem> maxBid(BigDecimal maxBid) {
		return maxBid == null ? noop()
				: (root, query, cb) -> cb.lessThanOrEqualTo(root.get("currentBidValue"), maxBid);
	}

	/** vehicleYear e texto ("2012" ou "2012/2013"); a comparacao lexical funciona p/ anos de 4 digitos. */
	private static Specification<AuctionItem> minYear(Integer minYear) {
		return minYear == null ? noop()
				: (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("vehicleYear"), minYear.toString());
	}

	private static Specification<AuctionItem> city(String city) {
		return isBlank(city) ? noop()
				: (root, query, cb) -> cb.like(cb.lower(root.join("auction").get("city")),
						"%" + city.toLowerCase() + "%");
	}

	private static Specification<AuctionItem> noop() {
		return (root, query, cb) -> cb.conjunction();
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
