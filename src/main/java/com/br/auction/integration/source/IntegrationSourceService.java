package com.br.auction.integration.source;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.br.auction.integration.credential.CredentialRepository;
import com.br.auction.integration.enums.ConnectorType;

import jakarta.persistence.EntityNotFoundException;

@Service
public class IntegrationSourceService {

	private final IntegrationSourceRepository repository;
	private final CredentialRepository credentialRepository;

	public IntegrationSourceService(IntegrationSourceRepository repository,
			CredentialRepository credentialRepository) {
		this.repository = repository;
		this.credentialRepository = credentialRepository;
	}

	public Page<IntegrationSource> findAll(String search, Pageable pageable) {
		return repository.findAll(buildSearch(search), pageable);
	}

	public IntegrationSource findById(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Fonte nao encontrada: " + id));
	}

	@Transactional
	public IntegrationSource create(IntegrationSourceRequest request) {
		if (repository.existsByCode(request.getCode())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ja existe uma fonte com o codigo " + request.getCode());
		}
		IntegrationSource source = new IntegrationSource();
		apply(source, request);
		return repository.save(source);
	}

	@Transactional
	public IntegrationSource update(Long id, IntegrationSourceRequest request) {
		IntegrationSource source = findById(id);
		if (repository.existsByCodeAndIdNot(request.getCode(), id)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ja existe uma fonte com o codigo " + request.getCode());
		}
		apply(source, request);
		return repository.save(source);
	}

	@Transactional
	public void delete(Long id) {
		IntegrationSource source = findById(id);
		repository.delete(source);
	}

	private void apply(IntegrationSource source, IntegrationSourceRequest request) {
		source.setCode(request.getCode());
		source.setName(request.getName());
		source.setDescription(request.getDescription());
		source.setConnectorType(request.getConnectorType());
		source.setBaseUrl(request.getBaseUrl());
		source.setJdbcUrl(request.getJdbcUrl());
		source.setJdbcDriver(request.getJdbcDriver());
		source.setProviderCode(request.getProviderCode());
		source.setProviderName(request.getProviderName());
		source.setStateCode(request.getStateCode());
		source.setStateName(request.getStateName());
		source.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());

		if (request.getCredentialId() != null) {
			source.setCredential(credentialRepository.findById(request.getCredentialId())
					.orElseThrow(() -> new EntityNotFoundException(
							"Credencial nao encontrada: " + request.getCredentialId())));
		} else {
			source.setCredential(null);
		}

		validateConnectorFields(request);
	}

	private void validateConnectorFields(IntegrationSourceRequest request) {
		if (request.getConnectorType() == ConnectorType.REST && isBlank(request.getBaseUrl())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A URL base e obrigatoria para fontes REST");
		}
		if (request.getConnectorType() == ConnectorType.JDBC && isBlank(request.getJdbcUrl())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A URL JDBC e obrigatoria para fontes JDBC");
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private Specification<IntegrationSource> buildSearch(String search) {
		if (search == null || search.isBlank()) {
			return null;
		}
		String like = "%" + search.trim().toLowerCase() + "%";
		return (root, query, cb) -> cb.or(
				cb.like(cb.lower(root.get("code")), like),
				cb.like(cb.lower(root.get("name")), like));
	}
}
