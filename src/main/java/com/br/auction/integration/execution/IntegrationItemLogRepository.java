package com.br.auction.integration.execution;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface IntegrationItemLogRepository extends JpaRepository<IntegrationItemLog, Long> {

	Page<IntegrationItemLog> findByRunId(Long runId, Pageable pageable);
}
