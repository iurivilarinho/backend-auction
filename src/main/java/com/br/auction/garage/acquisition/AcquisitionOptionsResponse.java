package com.br.auction.garage.acquisition;

import java.util.List;

import com.br.auction.response.EnumOptionResponse;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Opções para os formulários da garagem: status de aquisição, tipos de gasto e tipos de documento.
 * Substitui o antigo {@code Map<String, Object>} por um modelo tipado, mantendo o mesmo JSON.
 */
@Schema(description = "Opções (status, tipos de gasto, tipos de documento) da garagem")
public class AcquisitionOptionsResponse {

	@Schema(description = "Status de aquisição disponíveis")
	private final List<EnumOptionResponse> statuses;

	@Schema(description = "Tipos de gasto disponíveis")
	private final List<EnumOptionResponse> expenseTypes;

	@Schema(description = "Tipos de documento disponíveis")
	private final List<EnumOptionResponse> documentTypes;

	public AcquisitionOptionsResponse(List<EnumOptionResponse> statuses, List<EnumOptionResponse> expenseTypes,
			List<EnumOptionResponse> documentTypes) {
		this.statuses = statuses;
		this.expenseTypes = expenseTypes;
		this.documentTypes = documentTypes;
	}

	public List<EnumOptionResponse> getStatuses() {
		return statuses;
	}

	public List<EnumOptionResponse> getExpenseTypes() {
		return expenseTypes;
	}

	public List<EnumOptionResponse> getDocumentTypes() {
		return documentTypes;
	}
}
