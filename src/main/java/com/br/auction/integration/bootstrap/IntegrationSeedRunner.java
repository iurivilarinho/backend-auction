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

		integrationRepository.findByCode("DETRAN_MG_AUCTIONS_IN")
				.ifPresentOrElse(this::reconcileSchedule,
						() -> integrationRepository.save(buildAuctionIntegration(provider, source, auctionModel)));
		integrationRepository.findByCode("DETRAN_MG_LOTS_IN")
				.ifPresentOrElse(this::reconcileSchedule,
						() -> integrationRepository.save(buildLotIntegration(provider, source, lotModel)));

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
		addMapping(integration, "currentBidValue", "currentBidValue", "MONEY_BR", false, 5);
		addMapping(integration, "imageUrls", "imageUrls", null, false, 6);
		return integration;
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
}
