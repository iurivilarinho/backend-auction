package com.br.auction.integration.execution;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.br.auction.integration.enums.RunStatus;
import com.br.auction.integration.enums.TriggerType;
import com.br.auction.integration.integration.Integration;
import com.br.auction.integration.integration.IntegrationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;

/**
 * Orquestra disparos e consultas de execucoes de integracao.
 */
@Service
public class IntegrationRunService {

	private final IntegrationService integrationService;
	private final IntegrationExecutor executor;
	private final IntegrationRunRepository runRepository;
	private final IntegrationItemLogRepository itemLogRepository;
	private final ObjectMapper objectMapper;

	public IntegrationRunService(IntegrationService integrationService, IntegrationExecutor executor,
			IntegrationRunRepository runRepository, IntegrationItemLogRepository itemLogRepository,
			ObjectMapper objectMapper) {
		this.integrationService = integrationService;
		this.executor = executor;
		this.runRepository = runRepository;
		this.itemLogRepository = itemLogRepository;
		this.objectMapper = objectMapper;
	}

	public IntegrationRun triggerManually(Long integrationId) {
		Integration integration = integrationService.findById(integrationId);
		validateExecutable(integration);
		// Abre a execucao ja como RUNNING e processa em segundo plano: a lista mostra o
		// progresso ("integrando...") enquanto a execucao acontece.
		IntegrationRun run = executor.startRun(integration, TriggerType.MANUAL);
		executor.executeRunAsync(run.getId(), true);
		return run;
	}

	public List<IntegrationRun> findRunningRuns() {
		return runRepository.findByStatusOrderByStartedAtDesc(RunStatus.RUNNING);
	}

	public IntegrationRun receiveInbound(String code, Object body) {
		Integration integration = integrationService.findByCode(code);
		List<Map<String, Object>> records = toRecords(body);
		return executor.executeInbound(integration, records, TriggerType.INBOUND);
	}

	public Page<IntegrationRun> findByIntegration(Long integrationId, Pageable pageable) {
		return runRepository.findByIntegrationId(integrationId, pageable);
	}

	public Page<IntegrationRun> findAllRuns(Pageable pageable) {
		return runRepository.findAllByOrderByStartedAtDesc(pageable);
	}

	public java.util.Map<String, Long> runSummary() {
		java.util.Map<String, Long> summary = new java.util.LinkedHashMap<>();
		for (RunStatus runStatus : RunStatus.values()) {
			summary.put(runStatus.name(), runRepository.countByStatus(runStatus));
		}
		return summary;
	}

	public IntegrationRun findRun(Long runId) {
		return runRepository.findById(runId)
				.orElseThrow(() -> new EntityNotFoundException("Execucao nao encontrada: " + runId));
	}

	public Page<IntegrationItemLog> findRunItems(Long runId, Pageable pageable) {
		return itemLogRepository.findByRunId(runId, pageable);
	}

	private void validateExecutable(Integration integration) {
		if (integration.getSource() == null || integration.getSourceModel() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Integracao sem fonte ou modelo da fonte configurados");
		}
		if (integration.getFieldMappings() == null || integration.getFieldMappings().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Integracao sem regras de de->para configuradas");
		}
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> toRecords(Object body) {
		if (body == null) {
			return List.of();
		}
		if (body instanceof List<?> list) {
			List<Map<String, Object>> records = new ArrayList<>(list.size());
			for (Object element : list) {
				records.add(objectMapper.convertValue(element, new TypeReference<Map<String, Object>>() {
				}));
			}
			return records;
		}
		return List.of(objectMapper.convertValue(body, new TypeReference<Map<String, Object>>() {
		}));
	}
}
