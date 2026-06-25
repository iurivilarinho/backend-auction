package com.br.auction.integration.integration;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IntegrationRepository
		extends JpaRepository<Integration, Long>, JpaSpecificationExecutor<Integration> {

	boolean existsByCode(String code);

	boolean existsByCodeAndIdNot(String code, Long id);

	@EntityGraph(attributePaths = "fieldMappings")
	Optional<Integration> findWithMappingsById(Long id);

	/**
	 * Carrega a integracao com os mapeamentos e inicializa as associacoes EAGER (fonte,
	 * modelo da fonte e credencial) para uso fora de transacao (execucao assincrona).
	 */
	@Query("select distinct i from Integration i left join fetch i.fieldMappings where i.id = :id")
	Optional<Integration> findForExecutionById(@Param("id") Long id);

	Optional<Integration> findByCode(String code);

	@EntityGraph(attributePaths = "fieldMappings")
	Optional<Integration> findWithMappingsByCode(String code);

	List<Integration> findByTriggerModeAndActiveTrue(com.br.auction.integration.enums.TriggerMode triggerMode);
}
