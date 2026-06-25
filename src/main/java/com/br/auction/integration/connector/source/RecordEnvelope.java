package com.br.auction.integration.connector.source;

import java.util.Map;

/**
 * Envelope de um registro coletado da fonte: chave de negocio, valor de watermark e o
 * payload bruto (estrutura JSON em Map).
 */
public record RecordEnvelope(
		String businessKey,
		String watermarkValue,
		Map<String, Object> payload) {
}
