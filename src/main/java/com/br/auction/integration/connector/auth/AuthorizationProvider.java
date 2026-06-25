package com.br.auction.integration.connector.auth;

import java.util.Map;

import com.br.auction.integration.credential.Credential;

/**
 * Resolve os headers HTTP de autenticacao a partir de uma credencial.
 */
public interface AuthorizationProvider {

	Map<String, String> resolveHeaders(Credential credential);
}
