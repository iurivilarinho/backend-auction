package com.br.auction.integration.connector.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import com.br.auction.integration.credential.Credential;

/**
 * Implementacao HTTP de {@link AuthorizationProvider}, cobrindo os tipos de credencial
 * usuais (Basic, Bearer e API Key). Modelado de forma extensivel para novos tipos.
 */
@Service
public class HttpAuthorizationProvider implements AuthorizationProvider {

	@Override
	public Map<String, String> resolveHeaders(Credential credential) {
		Map<String, String> headers = new HashMap<>();
		if (credential == null || credential.getType() == null) {
			return headers;
		}
		switch (credential.getType()) {
			case BASIC -> {
				String raw = nullSafe(credential.getUsername()) + ":" + nullSafe(credential.getPassword());
				String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
				headers.put(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
			}
			case BEARER -> headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + nullSafe(credential.getToken()));
			case API_KEY -> {
				String headerName = credential.getApiKeyHeader() == null || credential.getApiKeyHeader().isBlank()
						? "X-API-Key"
						: credential.getApiKeyHeader();
				headers.put(headerName, nullSafe(credential.getApiKeyValue()));
			}
			case NONE, JDBC_USERPASS -> {
				// Sem header de autenticacao.
			}
		}
		return headers;
	}

	private String nullSafe(String value) {
		return value == null ? "" : value;
	}
}
