package com.br.auction.integration.execution;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.br.auction.integration.connector.ConnectorRegistry;
import com.br.auction.integration.connector.sink.InternalDestinationSender;
import com.br.auction.integration.connector.sink.SendResult;
import com.br.auction.integration.connector.source.RecordEnvelope;
import com.br.auction.integration.connector.source.SourceFetcher;
import com.br.auction.integration.connector.source.SourceFetcher.FetchContext;
import com.br.auction.integration.connector.util.FieldMapper;
import com.br.auction.integration.connector.util.JsonPaths;
import com.br.auction.integration.credential.Credential;
import com.br.auction.integration.enums.FetchMode;
import com.br.auction.integration.enums.ItemStatus;
import com.br.auction.integration.enums.RunStatus;
import com.br.auction.integration.enums.TriggerType;
import com.br.auction.integration.integration.Integration;
import com.br.auction.integration.integration.IntegrationRepository;
import com.br.auction.integration.mapping.FieldMapping;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Orquestra a execucao de uma integracao: coleta os registros da fonte, aplica o de->para
 * e grava nos modelos internos de destino, registrando contadores e logs por item.
 *
 * <p>Execucoes manuais/agendadas rodam de forma assincrona: a execucao e criada ja com
 * status RUNNING e os contadores sao atualizados ao longo do processamento, permitindo que
 * a lista mostre o progresso ("integrando...") em tempo real. O recebimento (inbound) roda
 * de forma sincrona.</p>
 */
@Service
public class IntegrationExecutor {

	private static final Logger LOG = LoggerFactory.getLogger(IntegrationExecutor.class);

	private final IntegrationRunRepository runRepository;
	private final IntegrationItemLogRepository itemLogRepository;
	private final IntegrationRepository integrationRepository;
	private final ConnectorRegistry connectorRegistry;
	private final FieldMapper fieldMapper;
	private final InternalDestinationSender destinationSender;
	private final ObjectMapper objectMapper;
	private final long manualItemDelayMs;

	public IntegrationExecutor(IntegrationRunRepository runRepository,
			IntegrationItemLogRepository itemLogRepository, IntegrationRepository integrationRepository,
			ConnectorRegistry connectorRegistry, FieldMapper fieldMapper,
			InternalDestinationSender destinationSender, ObjectMapper objectMapper,
			@Value("${integration.run.manual.item-delay-ms:350}") long manualItemDelayMs) {
		this.runRepository = runRepository;
		this.itemLogRepository = itemLogRepository;
		this.integrationRepository = integrationRepository;
		this.connectorRegistry = connectorRegistry;
		this.fieldMapper = fieldMapper;
		this.destinationSender = destinationSender;
		this.objectMapper = objectMapper;
		this.manualItemDelayMs = manualItemDelayMs;
	}

	/**
	 * Abre uma execucao com status RUNNING e a persiste imediatamente, para que ela ja
	 * apareca como "em execucao" antes do processamento comecar.
	 */
	@Transactional
	public IntegrationRun startRun(Integration integration, TriggerType triggerType) {
		return openRun(integration, triggerType);
	}

	/**
	 * Processa a execucao de forma assincrona (coleta da fonte + de->para + gravacao),
	 * atualizando os contadores ao longo do caminho.
	 */
	@Async
	public void executeRunAsync(Long runId, boolean paced) {
		IntegrationRun run = runRepository.findById(runId).orElse(null);
		if (run == null) {
			return;
		}
		Integration integration = integrationRepository.findForExecutionById(run.getIntegration().getId()).orElse(null);
		if (integration == null) {
			run.setStatus(RunStatus.FAILED);
			run.setErrorMessage("Integracao nao encontrada");
			finish(run);
			return;
		}
		ExecutionState state = new ExecutionState(integration.getWatermarkValue());
		try {
			SourceFetcher fetcher = connectorRegistry.resolveFetcher(integration.getSourceModel().getConnectorType());
			FetchContext context = new FetchContext(integration.getSource(), integration.getSourceModel(),
					resolveCredential(integration), integration.getWatermarkValue(),
					integration.getBatchSize() == null ? 100 : integration.getBatchSize());
			fetcher.fetch(context, batch -> processBatch(run, integration, batch, state, paced));
			closeRun(run, state, integration);
		} catch (RuntimeException ex) {
			failRun(run, state, ex);
		}
	}

	@Transactional
	public IntegrationRun executeInbound(Integration integration, List<Map<String, Object>> records,
			TriggerType triggerType) {
		IntegrationRun run = openRun(integration, triggerType);
		ExecutionState state = new ExecutionState(integration.getWatermarkValue());
		try {
			processBatch(run, integration, toEnvelopes(integration, records), state, false);
			closeRun(run, state, integration);
		} catch (RuntimeException ex) {
			failRun(run, state, ex);
		}
		return run;
	}

	private void processBatch(IntegrationRun run, Integration integration, List<RecordEnvelope> batch,
			ExecutionState state, boolean paced) {
		List<FieldMapping> mappings = integration.getFieldMappings();
		for (RecordEnvelope envelope : batch) {
			pace(paced);
			long startedAt = System.currentTimeMillis();
			IntegrationItemLog log = new IntegrationItemLog();
			log.setRun(run);
			log.setBusinessKey(envelope.businessKey());
			log.setSourcePayload(serialize(envelope.payload()));
			state.total++;

			try {
				if (envelope.businessKey() != null && !state.seenKeys.add(envelope.businessKey())) {
					finishItem(log, ItemStatus.SKIPPED, "Registro duplicado no lote/execucao", startedAt);
					state.skipped++;
					updateProgress(run, state);
					continue;
				}
				Map<String, Object> mapped = fieldMapper.map(envelope.payload(), mappings);
				log.setTargetPayload(serialize(mapped));
				SendResult result = destinationSender.send(integration.getTargetModel(), integration.getSource(),
						envelope.businessKey(), mapped);
				if (result.success()) {
					finishItem(log, ItemStatus.SUCCESS, null, startedAt);
					state.success++;
					updateWatermark(state, envelope.watermarkValue());
				} else {
					finishItem(log, ItemStatus.FAILED, result.errorMessage(), startedAt);
					state.failure++;
				}
			} catch (RuntimeException ex) {
				finishItem(log, ItemStatus.FAILED, ex.getMessage(), startedAt);
				state.failure++;
			}
			updateProgress(run, state);
		}
	}

	private void pace(boolean paced) {
		if (!paced || manualItemDelayMs <= 0) {
			return;
		}
		try {
			Thread.sleep(manualItemDelayMs);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	private void updateProgress(IntegrationRun run, ExecutionState state) {
		applyCounters(run, state);
		runRepository.save(run);
	}

	private void finishItem(IntegrationItemLog log, ItemStatus status, String error, long startedAt) {
		log.setStatus(status);
		log.setErrorMessage(error);
		log.setDurationMs(System.currentTimeMillis() - startedAt);
		itemLogRepository.save(log);
	}

	private List<RecordEnvelope> toEnvelopes(Integration integration, List<Map<String, Object>> records) {
		String businessKeyField = integration.getSourceModel() == null ? null
				: integration.getSourceModel().getBusinessKeyField();
		String watermarkField = integration.getSourceModel() == null ? null
				: integration.getSourceModel().getWatermarkField();
		List<RecordEnvelope> envelopes = new ArrayList<>(records.size());
		for (Map<String, Object> record : records) {
			String businessKey = businessKeyField == null ? null : stringOrNull(JsonPaths.get(record, businessKeyField));
			String watermark = watermarkField == null ? null : stringOrNull(JsonPaths.get(record, watermarkField));
			envelopes.add(new RecordEnvelope(businessKey, watermark, record));
		}
		return envelopes;
	}

	private IntegrationRun openRun(Integration integration, TriggerType triggerType) {
		IntegrationRun run = new IntegrationRun();
		run.setIntegration(integration);
		run.setStatus(RunStatus.RUNNING);
		run.setTriggerType(triggerType);
		run.setStartedAt(LocalDateTime.now());
		run.setWatermarkBefore(integration.getWatermarkValue());
		run.setTotalRecords(0);
		run.setSuccessCount(0);
		run.setFailureCount(0);
		run.setSkippedCount(0);
		return runRepository.save(run);
	}

	private void closeRun(IntegrationRun run, ExecutionState state, Integration integration) {
		applyCounters(run, state);
		RunStatus status;
		if (state.failure > 0 && state.success > 0) {
			status = RunStatus.PARTIAL;
		} else if (state.failure > 0 && state.success == 0 && state.total > 0) {
			status = RunStatus.FAILED;
		} else {
			status = RunStatus.SUCCESS;
		}
		run.setStatus(status);
		run.setWatermarkAfter(state.maxWatermark);
		finish(run);
		if (integration.getFetchMode() == FetchMode.INCREMENTAL && state.maxWatermark != null) {
			integration.setWatermarkValue(state.maxWatermark);
			integrationRepository.save(integration);
		}
		LOG.info("Execucao {} da integracao {} finalizada: status={} total={} sucesso={} falha={} ignorado={}",
				run.getId(), integration.getCode(), status, state.total, state.success, state.failure, state.skipped);
	}

	private void failRun(IntegrationRun run, ExecutionState state, RuntimeException ex) {
		LOG.error("Falha na execucao da integracao", ex);
		applyCounters(run, state);
		run.setStatus(RunStatus.FAILED);
		run.setErrorMessage(ex.getMessage());
		run.setWatermarkAfter(state.maxWatermark);
		finish(run);
	}

	private void applyCounters(IntegrationRun run, ExecutionState state) {
		run.setTotalRecords(state.total);
		run.setSuccessCount(state.success);
		run.setFailureCount(state.failure);
		run.setSkippedCount(state.skipped);
	}

	private void finish(IntegrationRun run) {
		run.setFinishedAt(LocalDateTime.now());
		if (run.getStartedAt() != null) {
			run.setDurationMs(Duration.between(run.getStartedAt(), run.getFinishedAt()).toMillis());
		}
		runRepository.save(run);
	}

	private void updateWatermark(ExecutionState state, String candidate) {
		if (candidate == null || candidate.isBlank()) {
			return;
		}
		if (state.maxWatermark == null || candidate.compareTo(state.maxWatermark) > 0) {
			state.maxWatermark = candidate;
		}
	}

	private Credential resolveCredential(Integration integration) {
		if (integration.getCredential() != null) {
			return integration.getCredential();
		}
		return integration.getSource() == null ? null : integration.getSource().getCredential();
	}

	private String serialize(Object value) {
		if (value == null) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(value);
		} catch (Exception ex) {
			return String.valueOf(value);
		}
	}

	private String stringOrNull(Object value) {
		return value == null ? null : String.valueOf(value).trim();
	}

	/**
	 * Estado mutavel acumulado durante uma execucao.
	 */
	private static final class ExecutionState {
		private int total;
		private int success;
		private int failure;
		private int skipped;
		private String maxWatermark;
		private final Set<String> seenKeys = new HashSet<>();

		private ExecutionState(String initialWatermark) {
			this.maxWatermark = initialWatermark;
		}
	}
}
