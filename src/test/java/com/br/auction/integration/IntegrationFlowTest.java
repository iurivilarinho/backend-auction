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

	private FieldMappingRequest mapping(String source, String target, String transform, boolean unique) {
		FieldMappingRequest mapping = new FieldMappingRequest();
		mapping.setSourceField(source);
		mapping.setTargetField(target);
		mapping.setTransform(transform);
		mapping.setUniqueKey(unique);
		return mapping;
	}
}
