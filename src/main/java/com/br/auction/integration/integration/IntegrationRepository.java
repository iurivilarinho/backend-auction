package com.br.auction.integration.integration;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IntegrationRepository
		extends JpaRepository<Integration, Long>, JpaSpecificationExecutor<Integration> {

	boolean existsByCode(String code);

	boolean existsByCodeAndIdNot(String code, Long id);

	@EntityGraph(attributePaths = "fieldMappings")
	Optional<Integration> findWithMappingsById(Long id);

	Optional<Integration> findByCode(String code);

	@EntityGraph(attributePaths = "fieldMappings")
	Optional<Integration> findWithMappingsByCode(String code);

	List<Integration> findByTriggerModeAndActiveTrue(com.br.auction.integration.enums.TriggerMode triggerMode);
}
