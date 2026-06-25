package com.br.auction.integration.mapping;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FieldMappingRepository extends JpaRepository<FieldMapping, Long> {

	List<FieldMapping> findByIntegrationIdOrderByOrdemAsc(Long integrationId);

	void deleteByIntegrationId(Long integrationId);
}
