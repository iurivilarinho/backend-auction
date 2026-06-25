package com.br.auction.integration.source;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IntegrationSourceRepository
		extends JpaRepository<IntegrationSource, Long>, JpaSpecificationExecutor<IntegrationSource> {

	boolean existsByCode(String code);

	boolean existsByCodeAndIdNot(String code, Long id);

	Optional<IntegrationSource> findByCode(String code);
}
