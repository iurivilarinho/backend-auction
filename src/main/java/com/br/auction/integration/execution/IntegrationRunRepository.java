package com.br.auction.integration.execution;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationRunRepository extends JpaRepository<IntegrationRun, Long> {

	Page<IntegrationRun> findByIntegrationId(Long integrationId, Pageable pageable);

	Optional<IntegrationRun> findTopByIntegrationIdOrderByStartedAtDesc(Long integrationId);

	long countByStatus(com.br.auction.integration.enums.RunStatus status);
}
