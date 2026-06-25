package com.br.auction.integration.model;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;

@Service
public class SourceModelService {

	private final SourceModelRepository repository;

	public SourceModelService(SourceModelRepository repository) {
		this.repository = repository;
	}

	public Page<SourceModel> findAll(String search, Pageable pageable) {
		return repository.findAll(buildSearch(search), pageable);
	}

	public SourceModel findById(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Modelo da fonte nao encontrado: " + id));
	}

	@Transactional
	public SourceModel create(SourceModelRequest request) {
		if (repository.existsByCode(request.getCode())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ja existe um modelo da fonte com o codigo " + request.getCode());
		}
		SourceModel model = new SourceModel();
		apply(model, request);
		return repository.save(model);
	}

	@Transactional
	public SourceModel update(Long id, SourceModelRequest request) {
		SourceModel model = findById(id);
		if (repository.existsByCodeAndIdNot(request.getCode(), id)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ja existe um modelo da fonte com o codigo " + request.getCode());
		}
		apply(model, request);
		return repository.save(model);
	}

	@Transactional
	public void delete(Long id) {
		SourceModel model = findById(id);
		repository.delete(model);
	}

	private void apply(SourceModel model, SourceModelRequest request) {
		model.setCode(request.getCode());
		model.setName(request.getName());
		model.setDescription(request.getDescription());
		model.setConnectorType(request.getConnectorType());
		model.setResourcePath(request.getResourcePath());
		model.setItemsJsonPath(request.getItemsJsonPath());
		model.setHasNextJsonPath(request.getHasNextJsonPath());
		model.setPageParamName(request.getPageParamName());
		model.setPageSizeParamName(request.getPageSizeParamName());
		model.setPageSize(request.getPageSize());
		model.setSourceMethod(request.getSourceMethod());
		model.setRequestBodyTemplate(request.getRequestBodyTemplate());
		model.setTableName(request.getTableName());
		model.setBusinessKeyField(request.getBusinessKeyField());
		model.setWatermarkField(request.getWatermarkField());
		model.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());

		applyFields(model, request);
	}

	private void applyFields(SourceModel model, SourceModelRequest request) {
		model.getFields().clear();
		if (request.getFields() == null) {
			return;
		}
		for (SourceModelFieldRequest fieldRequest : request.getFields()) {
			SourceModelField field = new SourceModelField();
			field.setCode(fieldRequest.getCode());
			field.setName(fieldRequest.getName());
			field.setDataType(fieldRequest.getDataType());
			field.setFormat(fieldRequest.getFormat());
			field.setRequired(fieldRequest.getRequired() == null ? Boolean.FALSE : fieldRequest.getRequired());
			field.setOrder(fieldRequest.getOrder());
			field.setDescription(fieldRequest.getDescription());
			field.setSourceModel(model);
			model.getFields().add(field);
		}
	}

	private Specification<SourceModel> buildSearch(String search) {
		if (search == null || search.isBlank()) {
			return null;
		}
		String like = "%" + search.trim().toLowerCase() + "%";
		return (root, query, cb) -> cb.or(
				cb.like(cb.lower(root.get("code")), like),
				cb.like(cb.lower(root.get("name")), like));
	}
}
