package com.br.auction.integration.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.br.auction.enums.AuctionProvider;
import com.br.auction.integration.credential.Credential;
import com.br.auction.integration.credential.CredentialRepository;
import com.br.auction.integration.enums.ConnectorType;
import com.br.auction.integration.enums.CredentialType;
import com.br.auction.integration.enums.FetchMode;
import com.br.auction.integration.enums.FieldDataType;
import com.br.auction.integration.enums.IntegrationStatus;
import com.br.auction.integration.enums.SourceMethod;
import com.br.auction.integration.enums.TriggerMode;
import com.br.auction.integration.integration.Integration;
import com.br.auction.integration.integration.IntegrationRepository;
import com.br.auction.integration.mapping.FieldMapping;
import com.br.auction.integration.model.SourceModel;
import com.br.auction.integration.model.SourceModelField;
import com.br.auction.integration.model.SourceModelRepository;
import com.br.auction.integration.source.IntegrationSource;
import com.br.auction.integration.source.IntegrationSourceRepository;
import com.br.auction.integration.target.InternalTargetModel;

/**
 * Garante (de forma idempotente, a cada inicializacao) que a credencial e as integracoes do
 * provedor inicial (DETRAN_MG) estejam sempre cadastradas. Toda informacao do provedor passa
 * pelo modulo de integracao: as integracoes consomem o scraping real exposto como feed JSON.
 */
@Component
@Order(20)
public class IntegrationSeedRunner implements CommandLineRunner {

	private static final Logger LOG = LoggerFactory.getLogger(IntegrationSeedRunner.class);
	private static final String CRON_EVERY_15_MIN = "0 0/15 * * * *";

	private final IntegrationSourceRepository sourceRepository;
	private final SourceModelRepository sourceModelRepository;
	private final IntegrationRepository integrationRepository;
	private final CredentialRepository credentialRepository;
	private final boolean seedEnabled;
	private final String selfUrl;
	private final String panelCpf;
	private final String panelPassword;

	public IntegrationSeedRunner(IntegrationSourceRepository sourceRepository,
			SourceModelRepository sourceModelRepository, IntegrationRepository integrationRepository,
			CredentialRepository credentialRepository,
			@Value("${integration.seed.enabled:true}") boolean seedEnabled,
			@Value("${app.self-url:http://localhost:8087}") String selfUrl,
			@Value("${detran.panel.cpf:}") String panelCpf,
			@Value("${detran.panel.password:}") String panelPassword) {
		this.sourceRepository = sourceRepository;
		this.sourceModelRepository = sourceModelRepository;
		this.integrationRepository = integrationRepository;
		this.credentialRepository = credentialRepository;
		this.seedEnabled = seedEnabled;
		this.selfUrl = selfUrl;
		this.panelCpf = panelCpf;
		this.panelPassword = panelPassword;
	}

	@Override
	@Transactional
	public void run(String... args) {
		if (!seedEnabled) {
			return;
		}

		AuctionProvider provider = AuctionProvider.defaultProvider();

		ensurePanelCredential();

		IntegrationSource source = sourceRepository.findByCode("DETRAN_MG_SOURCE")
				.orElseGet(() -> sourceRepository.save(buildSource(provider)));
		SourceModel auctionModel = sourceModelRepository.findByCode("DETRAN_MG_AUCTIONS")
				.orElseGet(() -> sourceModelRepository.save(buildAuctionModel()));
		SourceModel lotModel = sourceModelRepository.findByCode("DETRAN_MG_LOTS")
				.orElseGet(() -> sourceModelRepository.save(buildLotModel()));
		SourceModel lotLiveModel = sourceModelRepository.findByCode("DETRAN_MG_LOTS_LIVE")
				.orElseGet(() -> sourceModelRepository.save(buildLotLiveModel()));

		integrationRepository.findByCode("DETRAN_MG_AUCTIONS_IN")
				.ifPresentOrElse(this::reconcileSchedule,
						() -> integrationRepository.save(buildAuctionIntegration(provider, source, auctionModel)));
		integrationRepository.findByCode("DETRAN_MG_LOTS_IN")
				.ifPresentOrElse(integration -> {
					reconcileSchedule(integration);
					reconcileLotFloorMapping(integration);
				}, () -> integrationRepository.save(buildLotIntegration(provider, source, lotModel)));
		integrationRepository.findByCode("DETRAN_MG_LOTS_LIVE_IN")
				.ifPresentOrElse(this::reconcileSchedule,
						() -> integrationRepository.save(buildLotLiveIntegration(provider, source, lotLiveModel)));

		seedLeilo();
		seedMcLeilao();

		LOG.info("Integracao com o provedor {} garantida (seed idempotente).", provider.getCode());
	}

	/**
	 * Garante que as integracoes do provedor permanecam agendadas (recorrentes a cada 15 min) e
	 * ativas, mesmo em bancos criados por versoes anteriores do seed (que as cadastravam como
	 * MANUAL). Reconciliacao idempotente: so persiste quando algo de fato muda.
	 */
	private void reconcileSchedule(Integration integration) {
		boolean changed = false;
		if (integration.getTriggerMode() != TriggerMode.SCHEDULED) {
			integration.setTriggerMode(TriggerMode.SCHEDULED);
			changed = true;
		}
		if (!CRON_EVERY_15_MIN.equals(integration.getCronExpression())) {
			integration.setCronExpression(CRON_EVERY_15_MIN);
			changed = true;
		}
		if (integration.getStatus() != IntegrationStatus.ACTIVE) {
			integration.setStatus(IntegrationStatus.ACTIVE);
			changed = true;
		}
		if (!Boolean.TRUE.equals(integration.getActive())) {
			integration.setActive(Boolean.TRUE);
			changed = true;
		}
		if (changed) {
			integrationRepository.save(integration);
			LOG.info("Integracao {} reconciliada para coleta recorrente (SCHEDULED, a cada 15 min).",
					integration.getCode());
		}
	}

	private void ensurePanelCredential() {
		if (panelCpf == null || panelCpf.isBlank() || credentialRepository.existsByCode("DETRAN_MG_PANEL")) {
			return;
		}
		Credential credential = new Credential();
		credential.setCode("DETRAN_MG_PANEL");
		credential.setName("Painel DETRAN-MG (login do arrematante)");
		credential.setType(CredentialType.BASIC);
		credential.setUsername(panelCpf);
		credential.setPassword(panelPassword);
		credential.setActive(Boolean.TRUE);
		credentialRepository.save(credential);
		LOG.info("Credencial do painel DETRAN-MG cadastrada automaticamente.");
	}

	private IntegrationSource buildSource(AuctionProvider provider) {
		IntegrationSource source = new IntegrationSource();
		source.setCode("DETRAN_MG_SOURCE");
		source.setName("Fonte " + provider.getName());
		source.setDescription("Provedor inicial da aplicacao (extensivel a novos estados/provedores)");
		source.setConnectorType(ConnectorType.REST);
		// Consome o provedor pelo proprio backend (scraping real exposto como JSON paginado).
		source.setBaseUrl(selfUrl);
		source.setProviderCode(provider.getCode());
		source.setProviderName(provider.getName());
		source.setStateCode(provider.getStateCode());
		source.setStateName(provider.getStateName());
		source.setActive(Boolean.TRUE);
		return source;
	}

	private SourceModel buildAuctionModel() {
		SourceModel model = new SourceModel();
		model.setCode("DETRAN_MG_AUCTIONS");
		model.setName("Leiloes do provedor");
		model.setDescription("Estrutura dos leiloes recebidos do provedor");
		model.setConnectorType(ConnectorType.REST);
		model.setResourcePath("api/feed/auctions");
		model.setItemsJsonPath("items");
		model.setHasNextJsonPath("hasNext");
		model.setSourceMethod(SourceMethod.GET);
		model.setBusinessKeyField("auctionId");
		addField(model, "auctionId", "ID do leilao", FieldDataType.STRING, true);
		addField(model, "auctionNoticeNumber", "Numero do edital", FieldDataType.STRING, false);
		addField(model, "city", "Cidade", FieldDataType.STRING, false);
		addField(model, "auctioneer", "Patio/Leiloeiro", FieldDataType.STRING, false);
		addField(model, "status", "Status", FieldDataType.STRING, false);
		addField(model, "closingDate", "Data de encerramento", FieldDataType.STRING, false);
		addField(model, "auctionYear", "Ano", FieldDataType.STRING, false);
		addField(model, "sourceUrl", "URL", FieldDataType.STRING, false);
		return model;
	}

	private SourceModel buildLotModel() {
		SourceModel model = new SourceModel();
		model.setCode("DETRAN_MG_LOTS");
		model.setName("Lotes do provedor");
		model.setDescription("Estrutura dos lotes/veiculos recebidos do provedor");
		model.setConnectorType(ConnectorType.REST);
		model.setResourcePath("api/feed/lots");
		model.setItemsJsonPath("lots");
		model.setHasNextJsonPath("hasNext");
		model.setSourceMethod(SourceMethod.GET);
		model.setPageSize(5000);
		model.setBusinessKeyField("lotId");
		addField(model, "lotId", "ID do lote", FieldDataType.STRING, true);
		addField(model, "auctionId", "ID do leilao pai", FieldDataType.STRING, true);
		addField(model, "lotNumber", "Numero do lote", FieldDataType.STRING, false);
		addField(model, "lotType", "Tipo do lote", FieldDataType.STRING, false);
		addField(model, "vehicleDescription", "Descricao do veiculo", FieldDataType.STRING, false);
		addField(model, "currentBidValue", "Valor do lance", FieldDataType.STRING, false);
		addField(model, "imageUrls", "Imagens do veiculo", FieldDataType.STRING, false);
		return model;
	}

	private void addField(SourceModel model, String code, String name, FieldDataType type, boolean required) {
		SourceModelField field = new SourceModelField();
		field.setSourceModel(model);
		field.setCode(code);
		field.setName(name);
		field.setDataType(type);
		field.setRequired(required);
		field.setOrder(model.getFields().size());
		model.getFields().add(field);
	}

	private Integration buildAuctionIntegration(AuctionProvider provider, IntegrationSource source, SourceModel model) {
		Integration integration = new Integration();
		integration.setCode("DETRAN_MG_AUCTIONS_IN");
		integration.setName("Leiloes " + provider.getName());
		integration.setDescription("Coleta os leiloes do provedor (a cada 15 min) no modelo interno Auction");
		integration.setSource(source);
		integration.setSourceModel(model);
		integration.setTargetModel(InternalTargetModel.AUCTION);
		integration.setTriggerMode(TriggerMode.SCHEDULED);
		integration.setCronExpression(CRON_EVERY_15_MIN);
		integration.setFetchMode(FetchMode.FULL);
		integration.setStatus(IntegrationStatus.ACTIVE);
		integration.setActive(Boolean.TRUE);
		addMapping(integration, "auctionId", "detranAuctionId", null, true, 0);
		addMapping(integration, "auctionNoticeNumber", "auctionNoticeNumber", null, false, 1);
		addMapping(integration, "city", "city", null, false, 2);
		addMapping(integration, "auctioneer", "auctioneer", null, false, 3);
		addMapping(integration, "status", "status", null, false, 4);
		addMapping(integration, "closingDate", "closingDate", null, false, 5);
		addMapping(integration, "auctionYear", "auctionYear", null, false, 6);
		addMapping(integration, "sourceUrl", "sourceUrl", null, false, 7);
		return integration;
	}

	private Integration buildLotIntegration(AuctionProvider provider, IntegrationSource source, SourceModel model) {
		Integration integration = new Integration();
		integration.setCode("DETRAN_MG_LOTS_IN");
		integration.setName("Lotes " + provider.getName());
		integration.setDescription("Coleta os lotes/veiculos do provedor (a cada 15 min) no modelo interno AuctionItem");
		integration.setSource(source);
		integration.setSourceModel(model);
		integration.setTargetModel(InternalTargetModel.AUCTION_ITEM);
		integration.setTriggerMode(TriggerMode.SCHEDULED);
		integration.setCronExpression(CRON_EVERY_15_MIN);
		integration.setFetchMode(FetchMode.FULL);
		integration.setStatus(IntegrationStatus.ACTIVE);
		integration.setActive(Boolean.TRUE);
		addMapping(integration, "auctionId", "auctionDetranId", null, true, 0);
		addMapping(integration, "lotId", "lotId", null, true, 1);
		addMapping(integration, "lotNumber", "lotNumber", null, false, 2);
		addMapping(integration, "lotType", "lotType", null, false, 3);
		addMapping(integration, "vehicleDescription", "vehicleDescription", null, false, 4);
		// O lance do HTML e o inicial/piso -> alimenta minimumBidValue (o sink mantem o menor).
		// O lance AO VIVO vem da integracao DETRAN_MG_LOTS_LIVE_IN.
		addMapping(integration, "currentBidValue", "minimumBidValue", "MONEY_BR", false, 5);
		addMapping(integration, "imageUrls", "imageUrls", null, false, 6);
		return integration;
	}

	private SourceModel buildLotLiveModel() {
		SourceModel model = new SourceModel();
		model.setCode("DETRAN_MG_LOTS_LIVE");
		model.setName("Lotes ao vivo do provedor");
		model.setDescription("Lance ao vivo, prazo por lote e status (coletados lote a lote)");
		model.setConnectorType(ConnectorType.REST);
		model.setResourcePath("api/feed/lots-live");
		model.setItemsJsonPath("lots");
		model.setHasNextJsonPath("hasNext");
		model.setSourceMethod(SourceMethod.GET);
		model.setPageSize(50);
		model.setBusinessKeyField("lotId");
		addField(model, "lotId", "ID do lote", FieldDataType.STRING, true);
		addField(model, "auctionId", "ID do leilao pai", FieldDataType.STRING, true);
		addField(model, "currentBidValue", "Lance atual (ao vivo)", FieldDataType.STRING, false);
		addField(model, "closingDate", "Encerramento do lote", FieldDataType.STRING, false);
		addField(model, "lotStatus", "Status do lote", FieldDataType.STRING, false);
		return model;
	}

	private Integration buildLotLiveIntegration(AuctionProvider provider, IntegrationSource source, SourceModel model) {
		Integration integration = new Integration();
		integration.setCode("DETRAN_MG_LOTS_LIVE_IN");
		integration.setName("Lotes ao vivo " + provider.getName());
		integration.setDescription(
				"Atualiza lance ao vivo, prazo por lote e status (a cada 15 min) no modelo interno AuctionItem");
		integration.setSource(source);
		integration.setSourceModel(model);
		integration.setTargetModel(InternalTargetModel.AUCTION_ITEM);
		integration.setTriggerMode(TriggerMode.SCHEDULED);
		integration.setCronExpression(CRON_EVERY_15_MIN);
		integration.setFetchMode(FetchMode.FULL);
		integration.setStatus(IntegrationStatus.ACTIVE);
		integration.setActive(Boolean.TRUE);
		addMapping(integration, "auctionId", "auctionDetranId", null, true, 0);
		addMapping(integration, "lotId", "lotId", null, true, 1);
		addMapping(integration, "currentBidValue", "currentBidValue", null, false, 2);
		addMapping(integration, "closingDate", "lotClosingDate", null, false, 3);
		addMapping(integration, "lotStatus", "lotStatus", null, false, 4);
		return integration;
	}

	/**
	 * Reconciliacao para bancos ja existentes: a integracao de lotes passou a mandar o lance do HTML
	 * para o piso (minimumBidValue), e nao mais para currentBidValue (que agora e do feed ao vivo).
	 */
	private void reconcileLotFloorMapping(Integration integration) {
		boolean changed = false;
		for (FieldMapping mapping : integration.getFieldMappings()) {
			if ("currentBidValue".equals(mapping.getSourceField())
					&& !"minimumBidValue".equals(mapping.getTargetField())) {
				mapping.setTargetField("minimumBidValue");
				mapping.setTransform("MONEY_BR");
				changed = true;
			}
		}
		if (changed) {
			integrationRepository.save(integration);
			LOG.info("Integracao {} reconciliada: lance do HTML agora alimenta o piso (minimumBidValue).",
					integration.getCode());
		}
	}

	/**
	 * Garante que integracoes de lote ja existentes ganhem os mapeamentos de enriquecimento adicionados
	 * depois (condicao CONSERVADO/SUCATA e ano estruturado), sem recriar a integracao. Idempotente.
	 */
	private void reconcileLotEnrichmentMappings(Integration integration) {
		boolean changed = addMappingIfMissing(integration, "condition", "condition", 10);
		changed |= addMappingIfMissing(integration, "vehicleYear", "vehicleYear", 11);
		if (changed) {
			integrationRepository.save(integration);
			LOG.info("Integracao {} reconciliada: mapeamentos condition/vehicleYear garantidos.",
					integration.getCode());
		}
	}

	/** Adiciona o mapeamento se ainda nao houver um para o mesmo targetField. Retorna true se mudou. */
	private boolean addMappingIfMissing(Integration integration, String sourceField, String targetField, int ordem) {
		boolean exists = integration.getFieldMappings().stream()
				.anyMatch(mapping -> targetField.equals(mapping.getTargetField()));
		if (exists) {
			return false;
		}
		addMapping(integration, sourceField, targetField, null, false, ordem);
		return true;
	}

	private void addMapping(Integration integration, String sourceField, String targetField, String transform,
			boolean uniqueKey, int ordem) {
		FieldMapping mapping = new FieldMapping();
		mapping.setIntegration(integration);
		mapping.setSourceField(sourceField);
		mapping.setTargetField(targetField);
		mapping.setTransform(transform);
		mapping.setRequired(uniqueKey);
		mapping.setUniqueKey(uniqueKey);
		mapping.setOrdem(ordem);
		integration.getFieldMappings().add(mapping);
	}

	// ---------------------------------- Provedor LEILO (Grupo Leilo / GO) ----------------------------------
	// Consome a API publica da Leilo via feed proprio (/api/feed/{auctions,lots}?providerCode=LEILO_GO).

	private void seedLeilo() {
		AuctionProvider leilo = AuctionProvider.LEILO_GO;
		IntegrationSource leiloSource = sourceRepository.findByCode("LEILO_GO_SOURCE")
				.orElseGet(() -> sourceRepository.save(buildLeiloSource(leilo)));
		SourceModel auctionModel = sourceModelRepository.findByCode("LEILO_GO_AUCTIONS")
				.orElseGet(() -> sourceModelRepository.save(buildLeiloAuctionModel()));
		SourceModel lotModel = sourceModelRepository.findByCode("LEILO_GO_LOTS")
				.orElseGet(() -> sourceModelRepository.save(buildLeiloLotModel()));
		integrationRepository.findByCode("LEILO_GO_AUCTIONS_IN")
				.ifPresentOrElse(this::reconcileSchedule,
						() -> integrationRepository.save(buildLeiloAuctionIntegration(leilo, leiloSource, auctionModel)));
		integrationRepository.findByCode("LEILO_GO_LOTS_IN")
				.ifPresentOrElse(integration -> {
					reconcileSchedule(integration);
					reconcileLotEnrichmentMappings(integration);
				}, () -> integrationRepository.save(buildLeiloLotIntegration(leilo, leiloSource, lotModel)));
	}

	private IntegrationSource buildLeiloSource(AuctionProvider provider) {
		IntegrationSource source = new IntegrationSource();
		source.setCode("LEILO_GO_SOURCE");
		source.setName("Fonte " + provider.getName());
		source.setDescription("Plataforma Leilo (API publica) consumida via feed do backend");
		source.setConnectorType(ConnectorType.REST);
		source.setBaseUrl(selfUrl);
		source.setProviderCode(provider.getCode());
		source.setProviderName(provider.getName());
		source.setStateCode(provider.getStateCode());
		source.setStateName(provider.getStateName());
		source.setActive(Boolean.TRUE);
		return source;
	}

	private SourceModel buildLeiloAuctionModel() {
		SourceModel model = new SourceModel();
		model.setCode("LEILO_GO_AUCTIONS");
		model.setName("Leiloes Leilo");
		model.setDescription("Leiloes da plataforma Leilo");
		model.setConnectorType(ConnectorType.REST);
		model.setResourcePath("api/feed/auctions?providerCode=LEILO_GO");
		model.setItemsJsonPath("items");
		model.setHasNextJsonPath("hasNext");
		model.setSourceMethod(SourceMethod.GET);
		model.setPageSize(100);
		model.setBusinessKeyField("auctionId");
		addField(model, "auctionId", "ID do leilao", FieldDataType.STRING, true);
		addField(model, "auctionNoticeNumber", "Nome do leilao", FieldDataType.STRING, false);
		addField(model, "city", "Cidade", FieldDataType.STRING, false);
		addField(model, "auctioneer", "Leiloeiro", FieldDataType.STRING, false);
		addField(model, "status", "Status", FieldDataType.STRING, false);
		addField(model, "closingDate", "Data", FieldDataType.STRING, false);
		addField(model, "sourceUrl", "URL/edital", FieldDataType.STRING, false);
		return model;
	}

	private SourceModel buildLeiloLotModel() {
		SourceModel model = new SourceModel();
		model.setCode("LEILO_GO_LOTS");
		model.setName("Lotes Leilo");
		model.setDescription("Lotes de veiculo da plataforma Leilo (lance ao vivo, prazo por lote, status)");
		model.setConnectorType(ConnectorType.REST);
		model.setResourcePath("api/feed/lots?providerCode=LEILO_GO");
		model.setItemsJsonPath("lots");
		model.setHasNextJsonPath("hasNext");
		model.setSourceMethod(SourceMethod.GET);
		model.setPageSize(100);
		model.setBusinessKeyField("lotId");
		addField(model, "lotId", "ID do lote", FieldDataType.STRING, true);
		addField(model, "auctionId", "ID do leilao pai", FieldDataType.STRING, true);
		addField(model, "lotNumber", "Numero do lote", FieldDataType.STRING, false);
		addField(model, "lotType", "Tipo do lote", FieldDataType.STRING, false);
		addField(model, "vehicleDescription", "Descricao do veiculo", FieldDataType.STRING, false);
		addField(model, "vehicleYear", "Ano do veiculo", FieldDataType.STRING, false);
		addField(model, "condition", "Condicao (CONSERVADO/SUCATA)", FieldDataType.STRING, false);
		addField(model, "currentBidValue", "Lance atual", FieldDataType.STRING, false);
		addField(model, "minimumBidValue", "Lance inicial", FieldDataType.STRING, false);
		addField(model, "closingDate", "Encerramento do lote", FieldDataType.STRING, false);
		addField(model, "lotStatus", "Status do lote", FieldDataType.STRING, false);
		addField(model, "imageUrls", "Imagens", FieldDataType.STRING, false);
		return model;
	}

	private Integration buildLeiloAuctionIntegration(AuctionProvider provider, IntegrationSource source, SourceModel model) {
		Integration integration = new Integration();
		integration.setCode("LEILO_GO_AUCTIONS_IN");
		integration.setName("Leiloes " + provider.getName());
		integration.setDescription("Coleta os leiloes da Leilo (a cada 15 min) no modelo interno Auction");
		integration.setSource(source);
		integration.setSourceModel(model);
		integration.setTargetModel(InternalTargetModel.AUCTION);
		integration.setTriggerMode(TriggerMode.SCHEDULED);
		integration.setCronExpression(CRON_EVERY_15_MIN);
		integration.setFetchMode(FetchMode.FULL);
		integration.setStatus(IntegrationStatus.ACTIVE);
		integration.setActive(Boolean.TRUE);
		addMapping(integration, "auctionId", "detranAuctionId", null, true, 0);
		addMapping(integration, "auctionNoticeNumber", "auctionNoticeNumber", null, false, 1);
		addMapping(integration, "city", "city", null, false, 2);
		addMapping(integration, "auctioneer", "auctioneer", null, false, 3);
		addMapping(integration, "status", "status", null, false, 4);
		addMapping(integration, "closingDate", "closingDate", null, false, 5);
		addMapping(integration, "sourceUrl", "sourceUrl", null, false, 6);
		return integration;
	}

	// ---------------------------------- Provedor MC LEILAO (GO) ----------------------------------

	private void seedMcLeilao() {
		AuctionProvider mc = AuctionProvider.MCLEILAO_GO;
		IntegrationSource src = sourceRepository.findByCode("MCLEILAO_GO_SOURCE")
				.orElseGet(() -> sourceRepository.save(buildPlatformSource(mc, "MCLEILAO_GO_SOURCE")));
		SourceModel auctionModel = sourceModelRepository.findByCode("MCLEILAO_GO_AUCTIONS")
				.orElseGet(() -> sourceModelRepository.save(buildPlatformAuctionModel("MCLEILAO_GO_AUCTIONS",
						"Leiloes MC Leilao", "api/feed/auctions?providerCode=MCLEILAO_GO")));
		SourceModel lotModel = sourceModelRepository.findByCode("MCLEILAO_GO_LOTS")
				.orElseGet(() -> sourceModelRepository.save(buildPlatformLotModel("MCLEILAO_GO_LOTS",
						"Lotes MC Leilao", "api/feed/lots?providerCode=MCLEILAO_GO")));
		integrationRepository.findByCode("MCLEILAO_GO_AUCTIONS_IN")
				.ifPresentOrElse(this::reconcileSchedule, () -> integrationRepository.save(
						buildPlatformAuctionIntegration(mc, src, auctionModel, "MCLEILAO_GO_AUCTIONS_IN")));
		integrationRepository.findByCode("MCLEILAO_GO_LOTS_IN")
				.ifPresentOrElse(integration -> {
					reconcileSchedule(integration);
					reconcileLotEnrichmentMappings(integration);
				}, () -> integrationRepository.save(
						buildPlatformLotIntegration(mc, src, lotModel, "MCLEILAO_GO_LOTS_IN")));
	}

	// ---------- Builders genericos de plataforma (feed proprio + framework de integracao) ----------

	private IntegrationSource buildPlatformSource(AuctionProvider provider, String code) {
		IntegrationSource source = new IntegrationSource();
		source.setCode(code);
		source.setName("Fonte " + provider.getName());
		source.setDescription("Plataforma " + provider.getName() + " (API publica) consumida via feed do backend");
		source.setConnectorType(ConnectorType.REST);
		source.setBaseUrl(selfUrl);
		source.setProviderCode(provider.getCode());
		source.setProviderName(provider.getName());
		source.setStateCode(provider.getStateCode());
		source.setStateName(provider.getStateName());
		source.setActive(Boolean.TRUE);
		return source;
	}

	private SourceModel buildPlatformAuctionModel(String code, String name, String resourcePath) {
		SourceModel model = new SourceModel();
		model.setCode(code);
		model.setName(name);
		model.setDescription(name);
		model.setConnectorType(ConnectorType.REST);
		model.setResourcePath(resourcePath);
		model.setItemsJsonPath("items");
		model.setHasNextJsonPath("hasNext");
		model.setSourceMethod(SourceMethod.GET);
		model.setPageSize(100);
		model.setBusinessKeyField("auctionId");
		addField(model, "auctionId", "ID do leilao", FieldDataType.STRING, true);
		addField(model, "auctionNoticeNumber", "Nome do leilao", FieldDataType.STRING, false);
		addField(model, "city", "Cidade", FieldDataType.STRING, false);
		addField(model, "auctioneer", "Leiloeiro/Comitente", FieldDataType.STRING, false);
		addField(model, "status", "Status", FieldDataType.STRING, false);
		addField(model, "closingDate", "Data", FieldDataType.STRING, false);
		addField(model, "sourceUrl", "URL/edital", FieldDataType.STRING, false);
		return model;
	}

	private SourceModel buildPlatformLotModel(String code, String name, String resourcePath) {
		SourceModel model = new SourceModel();
		model.setCode(code);
		model.setName(name);
		model.setDescription(name);
		model.setConnectorType(ConnectorType.REST);
		model.setResourcePath(resourcePath);
		model.setItemsJsonPath("lots");
		model.setHasNextJsonPath("hasNext");
		model.setSourceMethod(SourceMethod.GET);
		model.setPageSize(100);
		model.setBusinessKeyField("lotId");
		addField(model, "lotId", "ID do lote", FieldDataType.STRING, true);
		addField(model, "auctionId", "ID do leilao pai", FieldDataType.STRING, true);
		addField(model, "lotNumber", "Numero do lote", FieldDataType.STRING, false);
		addField(model, "lotType", "Tipo do lote", FieldDataType.STRING, false);
		addField(model, "vehicleDescription", "Descricao do veiculo", FieldDataType.STRING, false);
		addField(model, "currentBidValue", "Lance atual", FieldDataType.STRING, false);
		addField(model, "minimumBidValue", "Lance inicial", FieldDataType.STRING, false);
		addField(model, "closingDate", "Encerramento do lote", FieldDataType.STRING, false);
		addField(model, "lotStatus", "Status do lote", FieldDataType.STRING, false);
		addField(model, "imageUrls", "Imagens", FieldDataType.STRING, false);
		return model;
	}

	private Integration buildPlatformAuctionIntegration(AuctionProvider provider, IntegrationSource source,
			SourceModel model, String code) {
		Integration integration = new Integration();
		integration.setCode(code);
		integration.setName("Leiloes " + provider.getName());
		integration.setDescription("Coleta os leiloes de " + provider.getName() + " (a cada 15 min) no modelo Auction");
		integration.setSource(source);
		integration.setSourceModel(model);
		integration.setTargetModel(InternalTargetModel.AUCTION);
		integration.setTriggerMode(TriggerMode.SCHEDULED);
		integration.setCronExpression(CRON_EVERY_15_MIN);
		integration.setFetchMode(FetchMode.FULL);
		integration.setStatus(IntegrationStatus.ACTIVE);
		integration.setActive(Boolean.TRUE);
		addMapping(integration, "auctionId", "detranAuctionId", null, true, 0);
		addMapping(integration, "auctionNoticeNumber", "auctionNoticeNumber", null, false, 1);
		addMapping(integration, "city", "city", null, false, 2);
		addMapping(integration, "auctioneer", "auctioneer", null, false, 3);
		addMapping(integration, "status", "status", null, false, 4);
		addMapping(integration, "closingDate", "closingDate", null, false, 5);
		addMapping(integration, "sourceUrl", "sourceUrl", null, false, 6);
		return integration;
	}

	private Integration buildPlatformLotIntegration(AuctionProvider provider, IntegrationSource source,
			SourceModel model, String code) {
		Integration integration = new Integration();
		integration.setCode(code);
		integration.setName("Lotes " + provider.getName());
		integration.setDescription("Coleta os lotes de veiculo de " + provider.getName() + " (a cada 15 min)");
		integration.setSource(source);
		integration.setSourceModel(model);
		integration.setTargetModel(InternalTargetModel.AUCTION_ITEM);
		integration.setTriggerMode(TriggerMode.SCHEDULED);
		integration.setCronExpression(CRON_EVERY_15_MIN);
		integration.setFetchMode(FetchMode.FULL);
		integration.setStatus(IntegrationStatus.ACTIVE);
		integration.setActive(Boolean.TRUE);
		addMapping(integration, "auctionId", "auctionDetranId", null, true, 0);
		addMapping(integration, "lotId", "lotId", null, true, 1);
		addMapping(integration, "lotNumber", "lotNumber", null, false, 2);
		addMapping(integration, "lotType", "lotType", null, false, 3);
		addMapping(integration, "vehicleDescription", "vehicleDescription", null, false, 4);
		addMapping(integration, "currentBidValue", "currentBidValue", null, false, 5);
		addMapping(integration, "minimumBidValue", "minimumBidValue", null, false, 6);
		addMapping(integration, "closingDate", "lotClosingDate", null, false, 7);
		addMapping(integration, "lotStatus", "lotStatus", null, false, 8);
		addMapping(integration, "imageUrls", "imageUrls", null, false, 9);
		addMapping(integration, "condition", "condition", null, false, 10);
		addMapping(integration, "vehicleYear", "vehicleYear", null, false, 11);
		return integration;
	}

	private Integration buildLeiloLotIntegration(AuctionProvider provider, IntegrationSource source, SourceModel model) {
		Integration integration = new Integration();
		integration.setCode("LEILO_GO_LOTS_IN");
		integration.setName("Lotes " + provider.getName());
		integration.setDescription("Coleta os lotes de veiculo da Leilo (a cada 15 min) no modelo interno AuctionItem");
		integration.setSource(source);
		integration.setSourceModel(model);
		integration.setTargetModel(InternalTargetModel.AUCTION_ITEM);
		integration.setTriggerMode(TriggerMode.SCHEDULED);
		integration.setCronExpression(CRON_EVERY_15_MIN);
		integration.setFetchMode(FetchMode.FULL);
		integration.setStatus(IntegrationStatus.ACTIVE);
		integration.setActive(Boolean.TRUE);
		addMapping(integration, "auctionId", "auctionDetranId", null, true, 0);
		addMapping(integration, "lotId", "lotId", null, true, 1);
		addMapping(integration, "lotNumber", "lotNumber", null, false, 2);
		addMapping(integration, "lotType", "lotType", null, false, 3);
		addMapping(integration, "vehicleDescription", "vehicleDescription", null, false, 4);
		addMapping(integration, "currentBidValue", "currentBidValue", null, false, 5);
		addMapping(integration, "minimumBidValue", "minimumBidValue", null, false, 6);
		addMapping(integration, "closingDate", "lotClosingDate", null, false, 7);
		addMapping(integration, "lotStatus", "lotStatus", null, false, 8);
		addMapping(integration, "imageUrls", "imageUrls", null, false, 9);
		addMapping(integration, "condition", "condition", null, false, 10);
		addMapping(integration, "vehicleYear", "vehicleYear", null, false, 11);
		return integration;
	}
}
