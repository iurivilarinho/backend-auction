package com.br.auction.integration.model;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceModelFieldRepository extends JpaRepository<SourceModelField, Long> {

	List<SourceModelField> findBySourceModelIdOrderByOrderAsc(Long sourceModelId);

	void deleteBySourceModelId(Long sourceModelId);
}
