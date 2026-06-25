package com.br.auction.integration.integration;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ja existe uma integracao com o codigo " + request.getCode());
		}
		Integration integration = new Integration();
		apply(integration, request);
		return repository.save(integration);
	}

	@Transactional
	public Integration update(Long id, IntegrationRequest request) {
		Integration integration = findById(id);
		if (repository.existsByCodeAndIdNot(request.getCode(), id)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ja existe uma integracao com o codigo " + request.getCode());
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
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
					"Transicao de status invalida: " + current + " -> " + target);
		}
		integration.setStatus(target);
		return repository.save(integration);
	}

	@Transactional
	public Integration clone(Long id) {
		Integration original = findById(id);
		Integration copy = new Integration();
		copy.setCode(generateCloneCode(original.getCode()));
		copy.setName(original.getName() + " (copia)");
		copy.setDescription(original.getDescription());
		copy.setSource(original.getSource());
		copy.setSourceModel(original.getSourceModel());
		copy.setCredential(original.getCredential());
		copy.setTargetModel(original.getTargetModel());
		copy.setTriggerMode(original.getTriggerMode());
		copy.setCronExpression(original.getCronExpression());
		copy.setFetchMode(original.getFetchMode());
		copy.setBatchSize(original.getBatchSize());
		copy.setStatus(IntegrationStatus.DRAFT);
		copy.setActive(Boolean.FALSE);
		for (FieldMapping mapping : original.getFieldMappings()) {
			FieldMapping clonedMapping = new FieldMapping();
			clonedMapping.setIntegration(copy);
			clonedMapping.setSourceField(mapping.getSourceField());
			clonedMapping.setTargetField(mapping.getTargetField());
			clonedMapping.setTransform(mapping.getTransform());
			clonedMapping.setDefaultValue(mapping.getDefaultValue());
			clonedMapping.setRequired(mapping.getRequired());
			clonedMapping.setUniqueKey(mapping.getUniqueKey());
			clonedMapping.setOrdem(mapping.getOrdem());
			copy.getFieldMappings().add(clonedMapping);
		}
		return repository.save(copy);
	}

	@Transactional
	public void delete(Long id) {
		Integration integration = findById(id);
		repository.delete(integration);
	}

	private String generateCloneCode(String base) {
		String candidate = base + "_COPY";
		int suffix = 2;
		while (repository.existsByCode(candidate)) {
			candidate = base + "_COPY" + suffix;
			suffix++;
		}
		return candidate;
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
