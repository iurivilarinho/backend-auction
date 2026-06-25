package com.br.auction.integration.bootstrap;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.br.auction.enums.AuctionProvider;
import com.br.auction.integration.enums.ConnectorType;
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
 * Cadastra/configura automaticamente a integracao com o provedor inicial da aplicacao
 * (DETRAN_MG) quando o banco esta vazio. Serve de configuracao de partida e de exemplo
 * completo de fonte -> de->para -> modelos internos, sem acoplar a aplicacao apenas a MG.
 */
@Component
@Order(20)
public class IntegrationSeedRunner implements CommandLineRunner {

	private static final Logger LOG = LoggerFactory.getLogger(IntegrationSeedRunner.class);

	private final IntegrationSourceRepository sourceRepository;
	private final SourceModelRepository sourceModelRepository;
	private final IntegrationRepository integrationRepository;
	private final boolean seedEnabled;

	public IntegrationSeedRunner(IntegrationSourceRepository sourceRepository,
			SourceModelRepository sourceModelRepository, IntegrationRepository integrationRepository,
			@Value("${integration.seed.enabled:true}") boolean seedEnabled) {
		this.sourceRepository = sourceRepository;
		this.sourceModelRepository = sourceModelRepository;
		this.integrationRepository = integrationRepository;
		this.seedEnabled = seedEnabled;
	}

	@Override
	@Transactional
	public void run(String... args) {
		if (!seedEnabled || integrationRepository.count() > 0) {
			return;
		}

		AuctionProvider provider = AuctionProvider.defaultProvider();
		IntegrationSource source = sourceRepository.findByCode("DETRAN_MG_SOURCE")
				.orElseGet(() -> sourceRepository.save(buildSource(provider)));

		SourceModel auctionModel = sourceModelRepository.findByCode("DETRAN_MG_AUCTIONS")
				.orElseGet(() -> sourceModelRepository.save(buildAuctionModel()));
		SourceModel lotModel = sourceModelRepository.findByCode("DETRAN_MG_LOTS")
				.orElseGet(() -> sourceModelRepository.save(buildLotModel()));

		integrationRepository.save(buildAuctionIntegration(provider, source, auctionModel));
		integrationRepository.save(buildLotIntegration(provider, source, lotModel));

		LOG.info("Integracao com o provedor {} cadastrada automaticamente (seed inicial).", provider.getCode());
	}

	private IntegrationSource buildSource(AuctionProvider provider) {
		IntegrationSource source = new IntegrationSource();
		source.setCode("DETRAN_MG_SOURCE");
		source.setName("Fonte " + provider.getName());
		source.setDescription("Provedor inicial da aplicacao (extensivel a novos estados/provedores)");
		source.setConnectorType(ConnectorType.REST);
		source.setBaseUrl(provider.getBaseUrl());
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
		model.setResourcePath("json/site/auctions");
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
		model.setResourcePath("json/site/details/auction");
		model.setItemsJsonPath("lots");
		model.setHasNextJsonPath("hasNext");
		model.setSourceMethod(SourceMethod.GET);
		model.setBusinessKeyField("lotId");
		addField(model, "lotId", "ID do lote", FieldDataType.STRING, true);
		addField(model, "auctionId", "ID do leilao pai", FieldDataType.STRING, true);
		addField(model, "lotNumber", "Numero do lote", FieldDataType.STRING, false);
		addField(model, "lotType", "Tipo do lote", FieldDataType.STRING, false);
		addField(model, "vehicleDescription", "Descricao do veiculo", FieldDataType.STRING, false);
		addField(model, "currentBidValue", "Valor do lance", FieldDataType.STRING, false);
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
		integration.setDescription("Integra os leiloes do provedor no modelo interno Auction");
		integration.setSource(source);
		integration.setSourceModel(model);
		integration.setTargetModel(InternalTargetModel.AUCTION);
		integration.setTriggerMode(TriggerMode.INBOUND);
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
		integration.setDescription("Integra os lotes/veiculos do provedor no modelo interno AuctionItem");
		integration.setSource(source);
		integration.setSourceModel(model);
		integration.setTargetModel(InternalTargetModel.AUCTION_ITEM);
		integration.setTriggerMode(TriggerMode.INBOUND);
		integration.setFetchMode(FetchMode.FULL);
		integration.setStatus(IntegrationStatus.ACTIVE);
		integration.setActive(Boolean.TRUE);
		addMapping(integration, "auctionId", "auctionDetranId", null, true, 0);
		addMapping(integration, "lotId", "lotId", null, true, 1);
		addMapping(integration, "lotNumber", "lotNumber", null, false, 2);
		addMapping(integration, "lotType", "lotType", null, false, 3);
		addMapping(integration, "vehicleDescription", "vehicleDescription", null, false, 4);
		addMapping(integration, "currentBidValue", "currentBidValue", "MONEY_BR", false, 5);
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

	public List<String> seededIntegrationCodes() {
		return List.of("DETRAN_MG_AUCTIONS_IN", "DETRAN_MG_LOTS_IN");
	}
}
