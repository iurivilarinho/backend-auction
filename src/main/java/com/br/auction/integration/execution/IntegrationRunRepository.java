package com.br.auction.integration.execution;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.br.auction.integration.enums.RunStatus;

public interface IntegrationRunRepository extends JpaRepository<IntegrationRun, Long> {

	Page<IntegrationRun> findByIntegrationId(Long integrationId, Pageable pageable);

	Page<IntegrationRun> findAllByOrderByStartedAtDesc(Pageable pageable);

	Optional<IntegrationRun> findTopByIntegrationIdOrderByStartedAtDesc(Long integrationId);

	List<IntegrationRun> findByStatusOrderByStartedAtDesc(RunStatus status);

	long countByStatus(RunStatus status);
}
