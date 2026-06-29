package com.br.auction.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.br.auction.models.CityCentroid;
import com.br.auction.repository.CityCentroidRepository;

/**
 * Carrega uma única vez a base offline de centroides dos municípios brasileiros (IBGE) a partir do
 * recurso {@code data/municipios-centroides.csv}. Idempotente: só roda quando a tabela está vazia.
 * Usa carga em lote (JdbcTemplate) por ser ~5,5 mil linhas.
 */
@Component
public class CityCentroidSeeder implements ApplicationRunner {

	private static final Logger LOG = LoggerFactory.getLogger(CityCentroidSeeder.class);
	private static final String RESOURCE = "data/municipios-centroides.csv";

	private final CityCentroidRepository repository;
	private final JdbcTemplate jdbcTemplate;

	public CityCentroidSeeder(CityCentroidRepository repository, JdbcTemplate jdbcTemplate) {
		this.repository = repository;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		if (repository.count() > 0) {
			return;
		}
		List<Object[]> rows = readRows();
		if (rows.isEmpty()) {
			LOG.warn("Seed de centroides ignorado: recurso {} vazio ou ausente.", RESOURCE);
			return;
		}
		jdbcTemplate.batchUpdate(
				"insert into tbCityCentroid (ibgeCode, name, normalizedName, uf, latitude, longitude) "
						+ "values (?, ?, ?, ?, ?, ?)",
				rows);
		LOG.info("Base de centroides carregada: {} municipios.", rows.size());
	}

	private List<Object[]> readRows() throws Exception {
		List<Object[]> rows = new ArrayList<>();
		ClassPathResource resource = new ClassPathResource(RESOURCE);
		if (!resource.exists()) {
			return rows;
		}
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
			reader.readLine(); // cabecalho: codigo_ibge,nome,uf,latitude,longitude
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank()) {
					continue;
				}
				String[] f = line.split(",");
				if (f.length < 5) {
					continue;
				}
				String name = f[1].trim();
				rows.add(new Object[] {
						Long.parseLong(f[0].trim()),
						name,
						CityCentroid.normalize(name),
						f[2].trim().toUpperCase(Locale.ROOT),
						Double.parseDouble(f[3].trim()),
						Double.parseDouble(f[4].trim()) });
			}
		}
		return rows;
	}
}
