package com.br.auction.integration.connector.source;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.br.auction.integration.credential.Credential;
import com.br.auction.integration.enums.ConnectorType;
import com.br.auction.integration.model.SourceModel;

import com.zaxxer.hikari.HikariDataSource;

/**
 * Coleta registros de uma fonte JDBC (tabela ou visao). Cada execucao abre um pool
 * dedicado de tamanho reduzido e percorre o resultado em lotes (streaming forward-only).
 */
@Component
public class JdbcSourceFetcher implements SourceFetcher {

	private static final Logger LOG = LoggerFactory.getLogger(JdbcSourceFetcher.class);

	@Override
	public ConnectorType supports() {
		return ConnectorType.JDBC;
	}

	@Override
	public void fetch(FetchContext context, Consumer<List<RecordEnvelope>> batchConsumer) {
		SourceModel model = context.model();
		String table = model.getTableName() != null && !model.getTableName().isBlank()
				? model.getTableName()
				: model.getResourcePath();
		if (table == null || table.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modelo da fonte JDBC sem tabela/recurso definido");
		}

		String sql = buildSql(table, model, context.watermarkValue());
		LOG.debug("Coleta JDBC sql={}", sql);

		try (HikariDataSource dataSource = buildDataSource(context);
				Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement()) {
			statement.setFetchSize(Math.max(context.batchSize(), 1));
			try (ResultSet rs = statement.executeQuery(sql)) {
				ResultSetMetaData meta = rs.getMetaData();
				int columnCount = meta.getColumnCount();
				List<RecordEnvelope> batch = new ArrayList<>(context.batchSize());
				while (rs.next()) {
					Map<String, Object> payload = new LinkedHashMap<>();
					for (int i = 1; i <= columnCount; i++) {
						payload.put(meta.getColumnLabel(i), normalize(rs.getObject(i)));
					}
					String businessKey = stringOrNull(payload.get(model.getBusinessKeyField()));
					String watermark = model.getWatermarkField() == null
							? null
							: stringOrNull(payload.get(model.getWatermarkField()));
					batch.add(new RecordEnvelope(businessKey, watermark, payload));
					if (batch.size() >= context.batchSize()) {
						batchConsumer.accept(new ArrayList<>(batch));
						batch.clear();
					}
				}
				if (!batch.isEmpty()) {
					batchConsumer.accept(batch);
				}
			}
		} catch (Exception ex) {
			throw new IllegalStateException("Falha ao coletar dados da fonte JDBC: " + ex.getMessage(), ex);
		}
	}

	private String buildSql(String table, SourceModel model, String watermarkValue) {
		StringBuilder sql = new StringBuilder("SELECT * FROM ").append(table);
		if (model.getWatermarkField() != null && !model.getWatermarkField().isBlank()
				&& watermarkValue != null && !watermarkValue.isBlank()) {
			sql.append(" WHERE ").append(model.getWatermarkField()).append(" > '")
					.append(watermarkValue.replace("'", "''")).append("'");
		}
		return sql.toString();
	}

	private HikariDataSource buildDataSource(FetchContext context) {
		HikariDataSource dataSource = new HikariDataSource();
		dataSource.setJdbcUrl(context.source().getJdbcUrl());
		if (context.source().getJdbcDriver() != null && !context.source().getJdbcDriver().isBlank()) {
			dataSource.setDriverClassName(context.source().getJdbcDriver());
		}
		Credential credential = context.credential();
		if (credential != null) {
			dataSource.setUsername(credential.getUsername());
			dataSource.setPassword(credential.getPassword());
		}
		dataSource.setMaximumPoolSize(2);
		dataSource.setReadOnly(true);
		return dataSource;
	}

	private Object normalize(Object value) {
		if (value instanceof Timestamp timestamp) {
			return timestamp.toLocalDateTime().toString();
		}
		return value;
	}

	private String stringOrNull(Object value) {
		return value == null ? null : String.valueOf(value).trim();
	}
}
