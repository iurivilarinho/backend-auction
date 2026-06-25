package com.br.auction.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.br.auction.integration.enums.ConnectorType;
import com.br.auction.integration.enums.RunStatus;
import com.br.auction.integration.enums.TriggerMode;
import com.br.auction.integration.execution.IntegrationRun;
import com.br.auction.integration.execution.IntegrationRunService;
import com.br.auction.integration.integration.IntegrationRequest;
import com.br.auction.integration.integration.IntegrationService;
import com.br.auction.integration.mapping.FieldMappingRequest;
import com.br.auction.integration.model.SourceModelRequest;
import com.br.auction.integration.model.SourceModelService;
import com.br.auction.integration.source.IntegrationSourceRequest;
import com.br.auction.integration.source.IntegrationSourceService;
import com.br.auction.integration.target.InternalTargetModel;
import com.br.auction.models.Auction;
import com.br.auction.models.AuctionItem;
import com.br.auction.repository.AuctionItemRepository;
import com.br.auction.repository.AuctionRepository;

/**
 * Valida o fluxo completo de integracao: recebimento (inbound) -> de->para -> gravacao no
 * modelo interno Auction, usando o banco H2 da aplicacao.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IntegrationFlowTest {

	@Autowired
	private IntegrationSourceService sourceService;

	@Autowired
	private SourceModelService sourceModelService;

	@Autowired
	private IntegrationService integrationService;

	@Autowired
	private IntegrationRunService runService;

	@Autowired
	private AuctionRepository auctionRepository;

	@Autowired
	private AuctionItemRepository auctionItemRepository;

	@Test
	void inboundFlowPersistsAuctionsIntoInternalModel() {
		IntegrationSourceRequest sourceRequest = new IntegrationSourceRequest();
		sourceRequest.setCode("TEST_SRC_MG");
		sourceRequest.setName("Fonte de teste MG");
		sourceRequest.setConnectorType(ConnectorType.REST);
		sourceRequest.setBaseUrl("https://leilao.detran.mg.gov.br");
		sourceRequest.setProviderCode("DETRAN_MG");
		sourceRequest.setProviderName("DETRAN Minas Gerais");
		sourceRequest.setStateCode("MG");
		sourceRequest.setStateName("Minas Gerais");
		Long sourceId = sourceService.create(sourceRequest).getId();

		SourceModelRequest modelRequest = new SourceModelRequest();
		modelRequest.setCode("TEST_MODEL_AUCTIONS");
		modelRequest.setName("Leiloes de teste");
		modelRequest.setConnectorType(ConnectorType.REST);
		modelRequest.setResourcePath("json/site/auctions");
		modelRequest.setBusinessKeyField("auctionId");
		Long modelId = sourceModelService.create(modelRequest).getId();

		IntegrationRequest integrationRequest = new IntegrationRequest();
		integrationRequest.setCode("TEST_AUCTIONS_IN");
		integrationRequest.setName("Integracao de teste");
		integrationRequest.setSourceId(sourceId);
		integrationRequest.setSourceModelId(modelId);
		integrationRequest.setTargetModel(InternalTargetModel.AUCTION);
		integrationRequest.setTriggerMode(TriggerMode.INBOUND);
		integrationRequest.setFieldMappings(List.of(
				mapping("auctionId", "detranAuctionId", null, true),
				mapping("auctionNoticeNumber", "auctionNoticeNumber", null, false),
				mapping("city", "city", "UPPER", false),
				mapping("status", "status", null, false)));
		integrationService.create(integrationRequest);

		List<Map<String, Object>> records = List.of(
				Map.of("auctionId", "9001", "auctionNoticeNumber", "001/2026", "city", "uberlandia", "status",
						"Publicado"),
				Map.of("auctionId", "9002", "auctionNoticeNumber", "002/2026", "city", "contagem", "status",
						"Em Andamento"));

		IntegrationRun run = runService.receiveInbound("TEST_AUCTIONS_IN", records);

		assertThat(run.getStatus()).isEqualTo(RunStatus.SUCCESS);
		assertThat(run.getSuccessCount()).isEqualTo(2);
		assertThat(run.getFailureCount()).isZero();

		Auction persisted = auctionRepository.findByProviderCodeAndDetranAuctionId("DETRAN_MG", "9001").orElseThrow();
		assertThat(persisted.getCity()).isEqualTo("UBERLANDIA");
		assertThat(persisted.getStateName()).isEqualTo("Minas Gerais");
		assertThat(persisted.getAuctionNoticeNumber()).isEqualTo("001/2026");
	}

	@Test
	void inboundFlowStoresVehicleImagesInDatabase() {
		IntegrationSourceRequest sourceRequest = new IntegrationSourceRequest();
		sourceRequest.setCode("TEST_SRC_IMG");
		sourceRequest.setName("Fonte de teste imagens");
		sourceRequest.setConnectorType(ConnectorType.REST);
		sourceRequest.setBaseUrl("https://leilao.detran.mg.gov.br");
		sourceRequest.setProviderCode("DETRAN_MG");
		sourceRequest.setProviderName("DETRAN Minas Gerais");
		sourceRequest.setStateCode("MG");
		sourceRequest.setStateName("Minas Gerais");
		Long sourceId = sourceService.create(sourceRequest).getId();

		SourceModelRequest modelRequest = new SourceModelRequest();
		modelRequest.setCode("TEST_MODEL_LOTS");
		modelRequest.setName("Lotes de teste");
		modelRequest.setConnectorType(ConnectorType.REST);
		modelRequest.setResourcePath("json/site/details/auction");
		modelRequest.setBusinessKeyField("lotId");
		Long modelId = sourceModelService.create(modelRequest).getId();

		IntegrationRequest integrationRequest = new IntegrationRequest();
		integrationRequest.setCode("TEST_LOTS_IN");
		integrationRequest.setName("Integracao de lotes de teste");
		integrationRequest.setSourceId(sourceId);
		integrationRequest.setSourceModelId(modelId);
		integrationRequest.setTargetModel(InternalTargetModel.AUCTION_ITEM);
		integrationRequest.setTriggerMode(TriggerMode.INBOUND);
		integrationRequest.setFieldMappings(List.of(
				mapping("auctionId", "auctionDetranId", null, false),
				mapping("lotId", "lotId", null, true),
				mapping("vehicleDescription", "vehicleDescription", null, false),
				mapping("currentBidValue", "currentBidValue", "MONEY_BR", false),
				mapping("imageUrls", "imageUrls", null, false)));
		integrationService.create(integrationRequest);

		// Leilao pai (destino interno) ja existente.
		Auction parent = new Auction();
		parent.setProviderCode("DETRAN_MG");
		parent.setProviderName("DETRAN Minas Gerais");
		parent.setStateCode("MG");
		parent.setStateName("Minas Gerais");
		parent.setDetranAuctionId("8001");
		Auction savedParent = auctionRepository.save(parent);

		// Imagem entregue como data URI (funciona offline e sem depender do provedor).
		String redDotPng = "data:image/png;base64,"
				+ "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

		List<Map<String, Object>> records = List.of(Map.of(
				"auctionId", "8001",
				"lotId", "LX-1",
				"vehicleDescription", "FIAT/UNO 2015",
				"currentBidValue", "R$ 12.500,00",
				"imageUrls", List.of(redDotPng, redDotPng)));

		IntegrationRun run = runService.receiveInbound("TEST_LOTS_IN", records);

		assertThat(run.getStatus()).isEqualTo(RunStatus.SUCCESS);
		assertThat(run.getSuccessCount()).isEqualTo(1);

		AuctionItem item = auctionItemRepository
				.findByAuctionIdAndLotId(savedParent.getId(), "LX-1").orElseThrow();
		assertThat(item.getImages()).hasSize(2);
		assertThat(item.getImages().get(0).getContentType()).isEqualTo("image/png");
		assertThat(item.getImages().get(0).getBytes()).isNotEmpty();
	}

	private FieldMappingRequest mapping(String source, String target, String transform, boolean unique) {
		FieldMappingRequest mapping = new FieldMappingRequest();
		mapping.setSourceField(source);
		mapping.setTargetField(target);
		mapping.setTransform(transform);
		mapping.setUniqueKey(unique);
		return mapping;
	}
}
