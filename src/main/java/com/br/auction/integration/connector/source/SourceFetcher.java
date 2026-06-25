package com.br.auction.integration.connector.source;

import java.util.List;
import java.util.function.Consumer;

import com.br.auction.integration.credential.Credential;
import com.br.auction.integration.enums.ConnectorType;
import com.br.auction.integration.model.SourceModel;
import com.br.auction.integration.source.IntegrationSource;

/**
 * Estrategia de coleta de dados de uma fonte. Implementacoes sao resolvidas por
 * {@link ConnectorType}, mantendo o fluxo de coleta extensivel a novos conectores.
 */
public interface SourceFetcher {

	ConnectorType supports();

	void fetch(FetchContext context, Consumer<List<RecordEnvelope>> batchConsumer);

	record FetchContext(
			IntegrationSource source,
			SourceModel model,
			Credential credential,
			String watermarkValue,
			int batchSize) {
	}
}
