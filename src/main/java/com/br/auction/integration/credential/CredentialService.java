package com.br.auction.integration.credential;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;

@Service
public class CredentialService {

	private final CredentialRepository repository;

	public CredentialService(CredentialRepository repository) {
		this.repository = repository;
	}

	public Page<Credential> findAll(String search, Pageable pageable) {
		return repository.findAll(buildSearch(search), pageable);
	}

	public Credential findById(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Credencial nao encontrada: " + id));
	}

	@Transactional
	public Credential create(CredentialRequest request) {
		if (repository.existsByCode(request.getCode())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ja existe uma credencial com o codigo " + request.getCode());
		}
		Credential credential = new Credential();
		apply(credential, request);
		return repository.save(credential);
	}

	@Transactional
	public Credential update(Long id, CredentialRequest request) {
		Credential credential = findById(id);
		if (repository.existsByCodeAndIdNot(request.getCode(), id)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ja existe uma credencial com o codigo " + request.getCode());
		}
		apply(credential, request);
		return repository.save(credential);
	}

	@Transactional
	public void delete(Long id) {
		Credential credential = findById(id);
		repository.delete(credential);
	}

	private void apply(Credential credential, CredentialRequest request) {
		credential.setCode(request.getCode());
		credential.setName(request.getName());
		credential.setType(request.getType());
		credential.setUsername(request.getUsername());
		credential.setPassword(request.getPassword());
		credential.setToken(request.getToken());
		credential.setApiKeyHeader(request.getApiKeyHeader());
		credential.setApiKeyValue(request.getApiKeyValue());
		credential.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
	}

	private Specification<Credential> buildSearch(String search) {
		if (search == null || search.isBlank()) {
			return null;
		}
		String like = "%" + search.trim().toLowerCase() + "%";
		return (root, query, cb) -> cb.or(
				cb.like(cb.lower(root.get("code")), like),
				cb.like(cb.lower(root.get("name")), like));
	}
}
