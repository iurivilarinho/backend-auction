package com.br.auction.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * O Hibernate 6 mapeia enums Java para o tipo nativo {@code ENUM(...)} do H2/MySQL. Como
 * {@code ddl-auto=update} nao altera o conjunto de valores do ENUM quando um novo valor e
 * adicionado ao enum Java (ex.: novo tipo de documento), inserir o valor novo falha. Aqui, no
 * startup, convertemos essas colunas para {@code VARCHAR}, removendo a restricao — a validacao do
 * enum continua sendo feita em Java. Best-effort e idempotente (rodar de novo nao causa dano).
 */
@Component
public class EnumCheckConstraintFixer {

	private static final Logger LOG = LoggerFactory.getLogger(EnumCheckConstraintFixer.class);

	/** Colunas de enum (tabela.coluna) a converter para VARCHAR. */
	private static final List<String[]> ENUM_COLUMNS = List.of(
			new String[] { "tbAcquisitionDocument", "type" },
			new String[] { "tbAcquisitionExpense", "type" },
			new String[] { "tbAcquisitionExpense", "status" },
			new String[] { "tbAcquisition", "status" });

	private final JdbcTemplate jdbcTemplate;

	public EnumCheckConstraintFixer(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void relaxEnumColumns() {
		for (String[] tc : ENUM_COLUMNS) {
			String table = tc[0];
			String column = tc[1];
			if (!isEnumColumn(table, column)) {
				continue; // ja e VARCHAR (ou tabela/coluna inexistente): nada a fazer
			}
			try {
				jdbcTemplate.execute("ALTER TABLE " + table + " ALTER COLUMN \"" + column + "\" SET DATA TYPE VARCHAR(40)");
				LOG.info("Fixup: coluna {}.{} convertida de ENUM para VARCHAR.", table, column);
			} catch (RuntimeException ex) {
				LOG.warn("Fixup: nao consegui converter {}.{} para VARCHAR: {}", table, column, ex.getMessage());
			}
		}
	}

	/** Verifica se a coluna ainda esta mapeada como ENUM nativo no banco. */
	private boolean isEnumColumn(String table, String column) {
		try {
			List<String> types = jdbcTemplate.queryForList(
					"SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS "
							+ "WHERE UPPER(TABLE_NAME) = UPPER(?) AND UPPER(COLUMN_NAME) = UPPER(?)",
					String.class, table, column);
			return types.stream().anyMatch(t -> t != null && t.toUpperCase().contains("ENUM"));
		} catch (RuntimeException ex) {
			LOG.debug("Fixup: nao consegui inspecionar {}.{}: {}", table, column, ex.getMessage());
			return false;
		}
	}
}
