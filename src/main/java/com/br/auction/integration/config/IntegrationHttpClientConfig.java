package com.br.auction.integration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP dedicado as integracoes. Mantido separado do restante da aplicacao
 * para permitir timeouts e configuracoes especificas de coleta de dados externos.
 */
@Configuration
public class IntegrationHttpClientConfig {

	private static final int CONNECT_TIMEOUT_MS = 30_000;
	// Generoso de proposito: a coleta de lotes varre todos os editais publicados em uma unica
	// chamada ao feed (cada edital e um scraping do provedor), entao precisa de folga.
	private static final int READ_TIMEOUT_MS = 120_000;

	@Bean
	public RestClient integrationRestClient() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
		factory.setReadTimeout(READ_TIMEOUT_MS);
		return RestClient.builder()
				.requestFactory(factory)
				.build();
	}
}
