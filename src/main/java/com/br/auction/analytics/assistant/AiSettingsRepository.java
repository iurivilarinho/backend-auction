package com.br.auction.analytics.assistant;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio do registro unico (singleton) de configuracao de IA. */
@Repository
public interface AiSettingsRepository extends JpaRepository<AiSettings, Long> {
}
