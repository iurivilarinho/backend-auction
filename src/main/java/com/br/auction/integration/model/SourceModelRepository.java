package com.br.auction.integration.model;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SourceModelRepository
		extends JpaRepository<SourceModel, Long>, JpaSpecificationExecutor<SourceModel> {

	boolean existsByCode(String code);

	boolean existsByCodeAndIdNot(String code, Long id);

	Optional<SourceModel> findByCode(String code);
}
