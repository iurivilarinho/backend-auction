package com.br.auction.integration.enums;

/**
 * Estrategia de autenticacao usada ao acessar a fonte externa. Modelado de forma
 * extensivel: novos tipos podem ser adicionados sem impactar o restante do fluxo.
 */
public enum CredentialType {

	NONE("NONE", "Sem autenticacao"),
	BASIC("BASIC", "HTTP Basic"),
	BEARER("BEARER", "Bearer token"),
	API_KEY("API_KEY", "API Key em header"),
	JDBC_USERPASS("JDBC_USERPASS", "Usuario e senha de banco");

	private final String code;
	private final String label;

	CredentialType(String code, String label) {
		this.code = code;
		this.label = label;
	}

	public String getCode() {
		return code;
	}

	public String getLabel() {
		return label;
	}
}
