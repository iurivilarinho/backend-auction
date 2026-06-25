package com.br.auction.integration.connector;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.br.auction.integration.connector.source.SourceFetcher;
import com.br.auction.integration.enums.ConnectorType;

/**
 * Registro dos conectores de coleta disponiveis, resolvidos por {@link ConnectorType}.
 * Novos conectores sao adicionados apenas implementando {@link SourceFetcher}.
 */
@Component
public class ConnectorRegistry {

	private final Map<ConnectorType, SourceFetcher> fetchers = new EnumMap<>(ConnectorType.class);

	public ConnectorRegistry(List<SourceFetcher> sourceFetchers) {
		for (SourceFetcher fetcher : sourceFetchers) {
			fetchers.put(fetcher.supports(), fetcher);
		}
	}

	public SourceFetcher resolveFetcher(ConnectorType type) {
		SourceFetcher fetcher = fetchers.get(type);
		if (fetcher == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conector de coleta nao suportado: " + type);
		}
		return fetcher;
	}
}
