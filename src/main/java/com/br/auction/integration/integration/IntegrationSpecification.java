package com.br.auction.integration.integration;

import org.springframework.data.jpa.domain.Specification;

import com.br.auction.integration.enums.IntegrationStatus;
import com.br.auction.integration.target.InternalTargetModel;

/**
 * Predicados dinamicos para a busca filtrada de integracoes.
 */
public final class IntegrationSpecification {

	private IntegrationSpecification() {
	}

	public static Specification<Integration> search(String term) {
		if (term == null || term.isBlank()) {
			return null;
		}
		String like = "%" + term.trim().toLowerCase() + "%";
		return (root, query, cb) -> cb.or(
				cb.like(cb.lower(root.get("code")), like),
				cb.like(cb.lower(root.get("name")), like),
				cb.like(cb.lower(root.get("description")), like));
	}

	public static Specification<Integration> sourceEquals(Long sourceId) {
		if (sourceId == null) {
			return null;
		}
		return (root, query, cb) -> cb.equal(root.get("source").get("id"), sourceId);
	}

	public static Specification<Integration> targetModelEquals(InternalTargetModel targetModel) {
		if (targetModel == null) {
			return null;
		}
		return (root, query, cb) -> cb.equal(root.get("targetModel"), targetModel);
	}

	public static Specification<Integration> statusEquals(IntegrationStatus status) {
		if (status == null) {
			return null;
		}
		return (root, query, cb) -> cb.equal(root.get("status"), status);
	}

	public static Specification<Integration> combine(Specification<Integration>... specs) {
		Specification<Integration> result = null;
		for (Specification<Integration> spec : specs) {
			if (spec != null) {
				result = result == null ? spec : result.and(spec);
			}
		}
		return result;
	}
}
