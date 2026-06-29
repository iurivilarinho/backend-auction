package com.br.auction.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.br.auction.enums.AuctionStatus;
import com.br.auction.enums.LotType;
import com.br.auction.models.AuctionItem;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Attribute.PersistentAttributeType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.Type.PersistenceType;

public class AuctionItemSpecification {

	public static Specification<AuctionItem> typeEquals(List<LotType> status) {
		if (status == null || status.isEmpty()) {

			return Specification.unrestricted();
		}
		return (root, query, criteriaBuilder) -> root.get("lotType")
				.in(status.stream().map(LotType::getDescription).toList());
	}

	public static Specification<AuctionItem> closedEquals(Boolean closed) {
		if (closed == null) {
			return Specification.unrestricted();
		}
		String finalizedDescription = AuctionStatus.FINALIZADO.getDescription();
		return (root, query, criteriaBuilder) -> {
			Path<?> statusPath = root.get("auction").get("status");
			return closed ? criteriaBuilder.equal(statusPath, finalizedDescription)
					: criteriaBuilder.or(criteriaBuilder.notEqual(statusPath, finalizedDescription),
							criteriaBuilder.isNull(statusPath));
		};
	}

	public static Specification<AuctionItem> auctionStatusEquals(List<AuctionStatus> status) {
		if (status == null || status.isEmpty()) {

			return Specification.unrestricted();
		}
		return (root, query, criteriaBuilder) -> root.get("auction").get("status")
				.in(status.stream().map(AuctionStatus::getDescription).toList());
	}

	public static Specification<AuctionItem> auctionIdEquals(Long auctionId) {
		if (auctionId == null) {
			return Specification.unrestricted();
		}
		return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("auction").get("id"), auctionId);
	}

	/** Filtra por um ou mais provedores; lista vazia/nula = todos os provedores. */
	public static Specification<AuctionItem> providerCodeIn(List<String> providerCodes) {
		List<String> codes = SpecSupport.normalize(providerCodes);
		if (codes.isEmpty()) {
			return Specification.unrestricted();
		}
		return (root, query, criteriaBuilder) -> root.get("auction").get("providerCode").in(codes);
	}

	public static Specification<AuctionItem> stateCodeEquals(String stateCode) {
		if (stateCode == null || stateCode.isBlank()) {
			return Specification.unrestricted();
		}
		return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("auction").get("stateCode"),
				stateCode.trim());
	}

	public static Specification<AuctionItem> brandIn(List<String> brands) {
		if (brands == null || brands.isEmpty()) {
			return Specification.unrestricted();
		}
		List<String> normalized = brands.stream().filter(b -> b != null && !b.isBlank())
				.map(b -> b.trim().toUpperCase()).toList();
		if (normalized.isEmpty()) {
			return Specification.unrestricted();
		}
		return (root, query, criteriaBuilder) -> criteriaBuilder.upper(root.get("brand")).in(normalized);
	}

	public static Specification<AuctionItem> yearIn(List<String> years) {
		if (years == null || years.isEmpty()) {
			return Specification.unrestricted();
		}
		List<String> normalized = years.stream().filter(y -> y != null && !y.isBlank()).map(String::trim).toList();
		if (normalized.isEmpty()) {
			return Specification.unrestricted();
		}
		return (root, query, criteriaBuilder) -> root.get("vehicleYear").in(normalized);
	}

	public static Specification<AuctionItem> modelContains(String model) {
		if (model == null || model.isBlank()) {
			return Specification.unrestricted();
		}
		String like = "%" + model.trim().toLowerCase() + "%";
		return (root, query, criteriaBuilder) -> criteriaBuilder.like(criteriaBuilder.lower(root.get("model")), like);
	}

	public static Specification<AuctionItem> bidBetween(java.math.BigDecimal min, java.math.BigDecimal max) {
		return valueBetween("currentBidValue", min, max);
	}

	public static Specification<AuctionItem> fipeBetween(java.math.BigDecimal min, java.math.BigDecimal max) {
		return valueBetween("fipeValue", min, max);
	}

	private static Specification<AuctionItem> valueBetween(String attribute, java.math.BigDecimal min,
			java.math.BigDecimal max) {
		if (min == null && max == null) {
			return Specification.unrestricted();
		}
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (min != null) {
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(attribute), min));
			}
			if (max != null) {
				predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(attribute), max));
			}
			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
	}

	public static Specification<AuctionItem> searchAllFields(String searchTerm, EntityManager entityManager) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (searchTerm == null || searchTerm.trim().isEmpty()) {
				return criteriaBuilder.conjunction();
			}

			// Termos para busca LIKE em strings
			String lowerSearchTerm = "%" + searchTerm.toLowerCase().trim() + "%";

			// Não precisamos de cleanSearchTerm, pois não há conversão numérica

			for (Attribute<?, ?> attribute : root.getModel().getAttributes()) {

				// --- 1. Busca em campos String da Entidade Root (TripContract) ---
				if (attribute.getJavaType().equals(String.class)
						&& attribute.getPersistentAttributeType() == PersistentAttributeType.BASIC) {

					Expression<String> stringExpression = root.get(attribute.getName()).as(String.class);
					predicates.add(criteriaBuilder.like(criteriaBuilder.lower(stringExpression), lowerSearchTerm));
				}

				// --- 2. Busca em Entidades Associadas (ManyToOne/OneToOne) ---
				if (attribute.isAssociation()) {
					if (attribute.getPersistentAttributeType() == PersistentAttributeType.MANY_TO_ONE
							|| attribute.getPersistentAttributeType() == PersistentAttributeType.ONE_TO_ONE) {

						String associationName = attribute.getName();
						Join<?, ?> join = root.join(associationName, JoinType.LEFT);

						Class<?> associatedClass = attribute.getJavaType();
						try {
							EntityType<?> associatedEntityType = entityManager.getMetamodel().entity(associatedClass);

							for (Attribute<?, ?> associatedAttribute : associatedEntityType.getAttributes()) {
								if (associatedAttribute.getJavaType().equals(String.class) && associatedAttribute
										.getPersistentAttributeType() == PersistentAttributeType.BASIC) {

									Expression<String> associatedStringExpression = join
											.get(associatedAttribute.getName()).as(String.class);
									predicates.add(criteriaBuilder
											.like(criteriaBuilder.lower(associatedStringExpression), lowerSearchTerm));
								}
							}
						} catch (IllegalArgumentException ignored) {
						}
					}
				}

				// --- 3. Tratamento para @ElementCollection (List<String>) ---
				if (attribute.isCollection() && attribute instanceof PluralAttribute) {
					PluralAttribute<?, ?, ?> pluralAttribute = (PluralAttribute<?, ?, ?>) attribute;

					if (pluralAttribute.getElementType().getPersistenceType() == PersistenceType.BASIC) {
						if (pluralAttribute.getElementType().getJavaType().equals(String.class)) {

							Join<?, ?> join = root.join(attribute.getName(), JoinType.LEFT);
							Expression<String> collectionExpression = join.as(String.class);

							predicates.add(
									criteriaBuilder.like(criteriaBuilder.lower(collectionExpression), lowerSearchTerm));
						}
					}
				}

				// --- 4. Busca em campos Numéricos (REMOVIDA) ---
				// Esta seção foi removida para evitar o erro 22003.
			}

			return predicates.isEmpty() ? criteriaBuilder.conjunction()
					: criteriaBuilder.or(predicates.toArray(new Predicate[0]));
		};
	}
}
