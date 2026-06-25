package com.br.auction.integration.target;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Modelo interno da aplicacao disponivel como destino fixo de integracao")
public class InternalTargetModelResponse {

	@Schema(description = "Codigo do modelo interno (AUCTION, AUCTION_ITEM)")
	private final String code;

	@Schema(description = "Rotulo do modelo interno")
	private final String label;

	@Schema(description = "Tabela interna correspondente")
	private final String tableName;

	@Schema(description = "Campo de chave de negocio do modelo")
	private final String businessKeyField;

	@Schema(description = "Campos disponiveis para o de->para")
	private final List<InternalTargetFieldResponse> fields;

	public InternalTargetModelResponse(InternalTargetModel model) {
		this.code = model.getCode();
		this.label = model.getLabel();
		this.tableName = model.getTableName();
		this.businessKeyField = model.getBusinessKeyField();
		this.fields = model.getFields().stream().map(InternalTargetFieldResponse::new).toList();
	}

	public String getCode() {
		return code;
	}

	public String getLabel() {
		return label;
	}

	public String getTableName() {
		return tableName;
	}

	public String getBusinessKeyField() {
		return businessKeyField;
	}

	public List<InternalTargetFieldResponse> getFields() {
		return fields;
	}
}
