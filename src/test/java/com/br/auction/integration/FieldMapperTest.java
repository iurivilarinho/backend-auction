package com.br.auction.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.br.auction.integration.connector.util.FieldMapper;
import com.br.auction.integration.mapping.FieldMapping;

class FieldMapperTest {

	private final FieldMapper fieldMapper = new FieldMapper();

	@Test
	void mapsAndTransformsSourceIntoTarget() {
		Map<String, Object> source = Map.of(
				"auctionId", " 123 ",
				"valor", "R$ 12.345,67",
				"city", "belo horizonte");

		List<FieldMapping> mappings = List.of(
				mapping("auctionId", "detranAuctionId", "TRIM", true),
				mapping("valor", "currentBidValue", "MONEY_BR", false),
				mapping("city", "city", "UPPER", false));

		Map<String, Object> result = fieldMapper.map(source, mappings);

		assertThat(result.get("detranAuctionId")).isEqualTo("123");
		assertThat(result.get("currentBidValue")).isEqualTo(new BigDecimal("12345.67"));
		assertThat(result.get("city")).isEqualTo("BELO HORIZONTE");
	}

	@Test
	void appliesDefaultValueWhenSourceMissing() {
		FieldMapping mapping = mapping("missing", "status", null, false);
		mapping.setDefaultValue("PUBLICADO");

		Map<String, Object> result = fieldMapper.map(Map.of(), List.of(mapping));

		assertThat(result.get("status")).isEqualTo("PUBLICADO");
	}

	private FieldMapping mapping(String source, String target, String transform, boolean required) {
		FieldMapping mapping = new FieldMapping();
		mapping.setSourceField(source);
		mapping.setTargetField(target);
		mapping.setTransform(transform);
		mapping.setRequired(required);
		return mapping;
	}
}
