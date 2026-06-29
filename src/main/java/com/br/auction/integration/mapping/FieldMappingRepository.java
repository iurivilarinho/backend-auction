package com.br.auction.integration.mapping;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface FieldMappingRepository extends JpaRepository<FieldMapping, Long> {

	List<FieldMapping> findByIntegrationIdOrderByOrdemAsc(Long integrationId);

	void deleteByIntegrationId(Long integrationId);
}
