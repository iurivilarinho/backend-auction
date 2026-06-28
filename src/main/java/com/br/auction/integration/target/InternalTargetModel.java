package com.br.auction.integration.target;

import java.util.List;

import com.br.auction.integration.enums.FieldDataType;

/**
 * Catalogo dos modelos internos da aplicacao que podem ser destino de uma integracao.
 *
 * <p>A aplicacao atual e sempre o destino da integracao: os modelos internos ja existem
 * e nao precisam ser cadastrados. Este enum expoe esses modelos e seus campos para que o
 * de->para da fonte aponte para campos reais do dominio.</p>
 */
public enum InternalTargetModel {

	AUCTION("AUCTION", "Leilao", "tbAuction", "detranAuctionId", List.of(
			InternalTargetField.key("detranAuctionId", "ID do leilao no provedor", FieldDataType.STRING,
					"Identificador do leilao na fonte. Chave de negocio do leilao."),
			InternalTargetField.of("auctionNoticeNumber", "Numero do edital", FieldDataType.STRING, null),
			InternalTargetField.of("city", "Cidade", FieldDataType.STRING, null),
			InternalTargetField.of("auctioneer", "Patio/Leiloeiro", FieldDataType.STRING, null),
			InternalTargetField.of("status", "Status no provedor", FieldDataType.STRING, null),
			InternalTargetField.of("closingDate", "Data de encerramento", FieldDataType.DATETIME,
					"Aceita dd/MM/yyyy HH:mm ou ISO-8601."),
			InternalTargetField.of("auctionYear", "Ano do leilao", FieldDataType.STRING, null),
			InternalTargetField.of("sourceUrl", "URL no provedor", FieldDataType.STRING, null))),

	AUCTION_ITEM("AUCTION_ITEM", "Item de leilao (veiculo)", "tbAuctionItem", "lotId", List.of(
			InternalTargetField.required("auctionDetranId", "ID do leilao pai no provedor", FieldDataType.STRING,
					"Usado para vincular o item ao leilao ja existente do mesmo provedor."),
			InternalTargetField.key("lotId", "ID do lote no provedor", FieldDataType.STRING,
					"Identificador do lote na fonte. Chave de negocio do item dentro do leilao."),
			InternalTargetField.of("lotNumber", "Numero do lote", FieldDataType.STRING, null),
			InternalTargetField.of("lotType", "Tipo do lote", FieldDataType.STRING, null),
			InternalTargetField.of("vehicleDescription", "Descricao do veiculo", FieldDataType.STRING, null),
			InternalTargetField.of("condition", "Condicao/origem do veiculo", FieldDataType.STRING,
					"Texto livre do provedor (ex.: Recuperado de Financiamento, Frota, Sucata), quando disponivel."),
			InternalTargetField.of("currentBidValue", "Valor do lance atual (ao vivo)", FieldDataType.DECIMAL, null),
			InternalTargetField.of("minimumBidValue", "Lance inicial/piso do lote", FieldDataType.DECIMAL,
					"Menor valor ja visto; o sink mantem sempre o menor (nunca sobe)."),
			InternalTargetField.of("lotClosingDate", "Encerramento do lote", FieldDataType.DATETIME,
					"Cada lote encerra em horario proprio. Aceita dd/MM/yyyy HH:mm ou ISO-8601."),
			InternalTargetField.of("lotStatus", "Status do lote no provedor", FieldDataType.STRING, null),
			InternalTargetField.of("fipeValue", "Valor FIPE", FieldDataType.DECIMAL, null),
			InternalTargetField.of("imageUrls", "Imagens do veiculo (URLs)", FieldDataType.STRING,
					"Aceita uma lista/array de URLs; as imagens sao baixadas e salvas no banco.")));

	private final String code;
	private final String label;
	private final String tableName;
	private final String businessKeyField;
	private final List<InternalTargetField> fields;

	InternalTargetModel(String code, String label, String tableName, String businessKeyField,
			List<InternalTargetField> fields) {
		this.code = code;
		this.label = label;
		this.tableName = tableName;
		this.businessKeyField = businessKeyField;
		this.fields = fields;
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

	public List<InternalTargetField> getFields() {
		return fields;
	}
}
