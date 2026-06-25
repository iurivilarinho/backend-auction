package com.br.auction.integration.integration;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.br.auction.integration.credential.Credential;
import com.br.auction.integration.credential.CredentialRepository;
import com.br.auction.integration.enums.FetchMode;
import com.br.auction.integration.enums.IntegrationStatus;
import com.br.auction.integration.enums.TriggerMode;
import com.br.auction.integration.mapping.FieldMapping;
import com.br.auction.integration.mapping.FieldMappingRequest;
import com.br.auction.integration.model.SourceModel;
import com.br.auction.integration.model.SourceModelRepository;
import com.br.auction.integration.source.IntegrationSource;
import com.br.auction.integration.source.IntegrationSourceRepository;
import com.br.auction.integration.target.InternalTargetModel;

import jakarta.persistence.EntityNotFoundException;

@Service
public class IntegrationService {

	private final IntegrationRepository repository;
	private final IntegrationSourceRepository sourceRepository;
	private final SourceModelRepository sourceModelRepository;
	private final CredentialRepository credentialRepository;

	public IntegrationService(IntegrationRepository repository, IntegrationSourceRepository sourceRepository,
			SourceModelRepository sourceModelRepository, CredentialRepository credentialRepository) {
		this.repository = repository;
		this.sourceRepository = sourceRepository;
		this.sourceModelRepository = sourceModelRepository;
		this.credentialRepository = credentialRepository;
	}

	public Page<Integration> findAll(String search, Long sourceId, InternalTargetModel targetModel,
			IntegrationStatus status, Pageable pageable) {
		Specification<Integration> spec = IntegrationSpecification.combine(
				IntegrationSpecification.search(search),
				IntegrationSpecification.sourceEquals(sourceId),
				IntegrationSpecification.targetModelEquals(targetModel),
				IntegrationSpecification.statusEquals(status));
		return repository.findAll(spec, pageable);
	}

	public Integration findById(Long id) {
		return repository.findWithMappingsById(id)
				.orElseThrow(() -> new EntityNotFoundException("Integracao nao encontrada: " + id));
	}

	public Integration findByCode(String code) {
		return repository.findWithMappingsByCode(code)
				.orElseThrow(() -> new EntityNotFoundException("Integracao nao encontrada: " + code));
	}

	@Transactional
	public Integration create(IntegrationRequest request) {
		if (repository.existsByCode(request.getCode())) {
			throw new IllegalArgumentException("Ja existe uma integracao com o codigo " + request.getCode());
		}
		Integration integration = new Integration();
		apply(integration, request);
		return repository.save(integration);
	}

	@Transactional
	public Integration update(Long id, IntegrationRequest request) {
		Integration integration = findById(id);
		if (repository.existsByCodeAndIdNot(request.getCode(), id)) {
			throw new IllegalArgumentException("Ja existe uma integracao com o codigo " + request.getCode());
		}
		apply(integration, request);
		return repository.save(integration);
	}

	@Transactional
	public Integration updateStatus(Long id, IntegrationStatus target) {
		Integration integration = findById(id);
		IntegrationStatus current = integration.getStatus();
		if (current == target) {
			return integration;
		}
		if (current == null || !current.canTransitionTo(target)) {
			throw new IllegalArgumentException(
					"Transicao de status invalida: " + current + " -> " + target);
		}
		integration.setStatus(target);
		return repository.save(integration);
	}

	@Transactional
	public void delete(Long id) {
		Integration integration = findById(id);
		repository.delete(integration);
	}

	private void apply(Integration integration, IntegrationRequest request) {
		integration.setCode(request.getCode());
		integration.setName(request.getName());
		integration.setDescription(request.getDescription());
		integration.setSource(resolveSource(request.getSourceId()));
		integration.setSourceModel(resolveSourceModel(request.getSourceModelId()));
		integration.setCredential(resolveCredential(request.getCredentialId()));
		integration.setTargetModel(request.getTargetModel());
		integration.setTriggerMode(request.getTriggerMode() == null ? TriggerMode.MANUAL : request.getTriggerMode());
		integration.setCronExpression(request.getCronExpression());
		integration.setFetchMode(request.getFetchMode() == null ? FetchMode.FULL : request.getFetchMode());
		if (request.getWatermarkValue() != null) {
			integration.setWatermarkValue(request.getWatermarkValue());
		}
		integration.setBatchSize(request.getBatchSize() == null ? 100 : request.getBatchSize());
		integration.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
		applyMappings(integration, request);
	}

	private void applyMappings(Integration integration, IntegrationRequest request) {
		integration.getFieldMappings().clear();
		if (request.getFieldMappings() == null) {
			return;
		}
		int order = 0;
		for (FieldMappingRequest mappingRequest : request.getFieldMappings()) {
			FieldMapping mapping = new FieldMapping();
			mapping.setIntegration(integration);
			mapping.setSourceField(mappingRequest.getSourceField());
			mapping.setTargetField(mappingRequest.getTargetField());
			mapping.setTransform(mappingRequest.getTransform());
			mapping.setDefaultValue(mappingRequest.getDefaultValue());
			mapping.setRequired(mappingRequest.getRequired() != null && mappingRequest.getRequired());
			mapping.setUniqueKey(mappingRequest.getUniqueKey() != null && mappingRequest.getUniqueKey());
			mapping.setOrdem(mappingRequest.getOrdem() != null ? mappingRequest.getOrdem() : order);
			integration.getFieldMappings().add(mapping);
			order++;
		}
	}

	private IntegrationSource resolveSource(Long sourceId) {
		return sourceRepository.findById(sourceId)
				.orElseThrow(() -> new EntityNotFoundException("Fonte nao encontrada: " + sourceId));
	}

	private SourceModel resolveSourceModel(Long sourceModelId) {
		return sourceModelRepository.findById(sourceModelId)
				.orElseThrow(() -> new EntityNotFoundException("Modelo da fonte nao encontrado: " + sourceModelId));
	}

	private Credential resolveCredential(Long credentialId) {
		if (credentialId == null) {
			return null;
		}
		return credentialRepository.findById(credentialId)
				.orElseThrow(() -> new EntityNotFoundException("Credencial nao encontrada: " + credentialId));
	}
}
