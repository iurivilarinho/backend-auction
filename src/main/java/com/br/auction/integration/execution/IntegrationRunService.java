package com.br.auction.integration.execution;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

	// Mapeia para DTO dentro da transacao: errorMessage/payloads sao @Lob (Large Object/OID no Postgres)
	// e so podem ser lidos com transacao ativa (fora dela: "Large Objects may not be used in auto-commit mode").
	@Transactional(readOnly = true)
	public List<IntegrationRunResponse> findRunningRuns() {
		return runRepository.findByStatusOrderByStartedAtDesc(RunStatus.RUNNING).stream()
				.map(IntegrationRunResponse::new)
				.toList();
	}

	public IntegrationRun receiveInbound(String code, Object body) {
		Integration integration = integrationService.findByCode(code);
		List<Map<String, Object>> records = toRecords(body);
		return executor.executeInbound(integration, records, TriggerType.INBOUND);
	}

	@Transactional(readOnly = true)
	public Page<IntegrationRunResponse> findByIntegration(Long integrationId, Pageable pageable) {
		return runRepository.findByIntegrationId(integrationId, pageable).map(IntegrationRunResponse::new);
	}

	@Transactional(readOnly = true)
	public Page<IntegrationRunResponse> findAllRuns(Pageable pageable) {
		return runRepository.findAllByOrderByStartedAtDesc(pageable).map(IntegrationRunResponse::new);
	}

	public IntegrationRunSummaryResponse runSummary() {
		return new IntegrationRunSummaryResponse(
				runRepository.countByStatus(RunStatus.RUNNING),
				runRepository.countByStatus(RunStatus.SUCCESS),
				runRepository.countByStatus(RunStatus.PARTIAL),
				runRepository.countByStatus(RunStatus.FAILED),
				runRepository.countByStatus(RunStatus.CANCELLED));
	}

	@Transactional(readOnly = true)
	public IntegrationRunResponse findRun(Long runId) {
		return runRepository.findById(runId)
				.map(IntegrationRunResponse::new)
				.orElseThrow(() -> new EntityNotFoundException("Execucao nao encontrada: " + runId));
	}

	@Transactional(readOnly = true)
	public Page<IntegrationItemLogResponse> findRunItems(Long runId, Pageable pageable) {
		return itemLogRepository.findByRunId(runId, pageable).map(IntegrationItemLogResponse::new);
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
