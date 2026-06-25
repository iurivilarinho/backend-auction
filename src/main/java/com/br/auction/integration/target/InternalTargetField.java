package com.br.auction.integration.target;

import com.br.auction.integration.enums.FieldDataType;

/**
 * Descritor de um campo de um modelo interno de destino. Os modelos internos ja
 * existem na aplicacao (Auction, AuctionItem) e sao tratados como destino fixo da
 * integracao, portanto nao precisam ser cadastrados como modelos externos.
 */
public record InternalTargetField(
		String code,
		String label,
		FieldDataType dataType,
		boolean required,
		boolean businessKey,
		String description) {

	public static InternalTargetField of(String code, String label, FieldDataType dataType, String description) {
		return new InternalTargetField(code, label, dataType, false, false, description);
	}

	public static InternalTargetField required(String code, String label, FieldDataType dataType, String description) {
		return new InternalTargetField(code, label, dataType, true, false, description);
	}

	public static InternalTargetField key(String code, String label, FieldDataType dataType, String description) {
		return new InternalTargetField(code, label, dataType, true, true, description);
	}
}
