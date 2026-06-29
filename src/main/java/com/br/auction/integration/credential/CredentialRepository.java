package com.br.auction.integration.credential;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface CredentialRepository
		extends JpaRepository<Credential, Long>, JpaSpecificationExecutor<Credential> {

	boolean existsByCode(String code);

	boolean existsByCodeAndIdNot(String code, Long id);

	Optional<Credential> findByCode(String code);
}
