package com.br.auction.integration.model;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface SourceModelFieldRepository extends JpaRepository<SourceModelField, Long> {

	List<SourceModelField> findBySourceModelIdOrderByOrderAsc(Long sourceModelId);

	void deleteBySourceModelId(Long sourceModelId);
}
