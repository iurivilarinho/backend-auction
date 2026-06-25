package com.br.auction.integration.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de vida de uma integracao. As transicoes sao validadas de forma centralizada
 * para evitar regras espalhadas pelo codigo.
 */
public enum IntegrationStatus {

	DRAFT("Rascunho"),
	ACTIVE("Ativa"),
	PAUSED("Pausada"),
	ARCHIVED("Arquivada");

	private final String description;

	IntegrationStatus(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public Set<IntegrationStatus> allowedTransitions() {
		return switch (this) {
			case DRAFT -> EnumSet.of(ACTIVE, ARCHIVED);
			case ACTIVE -> EnumSet.of(PAUSED, ARCHIVED);
			case PAUSED -> EnumSet.of(ACTIVE, ARCHIVED);
			case ARCHIVED -> EnumSet.noneOf(IntegrationStatus.class);
		};
	}

	public boolean canTransitionTo(IntegrationStatus target) {
		return target != null && allowedTransitions().contains(target);
	}
}
