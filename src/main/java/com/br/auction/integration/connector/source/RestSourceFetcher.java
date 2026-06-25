package com.br.auction.integration.connector.source;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.br.auction.integration.connector.auth.AuthorizationProvider;
import com.br.auction.integration.connector.util.JsonPaths;
import com.br.auction.integration.enums.ConnectorType;
import com.br.auction.integration.enums.SourceMethod;
import com.br.auction.integration.model.SourceModel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Coleta registros de uma fonte REST paginada. Resolve autenticacao, monta a URL com os
 * parametros de paginacao do modelo e extrai os itens pelo caminho JSON declarado.
 */
@Component
public class RestSourceFetcher implements SourceFetcher {

	private static final Logger LOG = LoggerFactory.getLogger(RestSourceFetcher.class);
	private static final int MAX_PAGES = 10_000;

	private final AuthorizationProvider authorizationProvider;
	private final ObjectMapper objectMapper;
	private final RestClient restClient;

	public RestSourceFetcher(AuthorizationProvider authorizationProvider, ObjectMapper objectMapper,
			RestClient integrationRestClient) {
		this.authorizationProvider = authorizationProvider;
		this.objectMapper = objectMapper;
		this.restClient = integrationRestClient;
	}

	@Override
	public ConnectorType supports() {
		return ConnectorType.REST;
	}

	@Override
	public void fetch(FetchContext context, Consumer<List<RecordEnvelope>> batchConsumer) {
		SourceModel model = context.model();
		String baseUrl = stripTrailingSlash(context.source().getBaseUrl());
		String path = trimSlashes(applyPlaceholders(model.getResourcePath(), context.watermarkValue()));
		Map<String, String> authHeaders = authorizationProvider.resolveHeaders(context.credential());
		SourceMethod method = model.getSourceMethod() == null ? SourceMethod.GET : model.getSourceMethod();

		int page = 1;
		boolean hasNext = true;
		while (hasNext && page <= MAX_PAGES) {
			URI uri = UriComponentsBuilder.fromUriString(baseUrl + "/" + path)
					.queryParam(model.getPageParamName(), page)
					.queryParam(model.getPageSizeParamName(), model.getPageSize())
					.build(true)
					.toUri();

			LOG.debug("Coleta REST method={} page={} uri={}", method, page, uri);
			byte[] bytes = executeRequest(uri, method, model, authHeaders, context);
			if (bytes == null || bytes.length == 0) {
				break;
			}
			Map<String, Object> response = parseJsonWithEncodingFallback(bytes);
			if (response == null) {
				break;
			}
			List<Map<String, Object>> items = JsonPaths.asItemList(JsonPaths.get(response, model.getItemsJsonPath()));
			List<RecordEnvelope> batch = new ArrayList<>(items.size());
			for (Map<String, Object> item : items) {
				String businessKey = stringOrNull(JsonPaths.get(item, model.getBusinessKeyField()));
				String watermark = model.getWatermarkField() == null
						? null
						: stringOrNull(JsonPaths.get(item, model.getWatermarkField()));
				batch.add(new RecordEnvelope(businessKey, watermark, item));
			}
			if (!batch.isEmpty()) {
				batchConsumer.accept(batch);
			}
			Object hasNextValue = JsonPaths.get(response, model.getHasNextJsonPath());
			hasNext = hasNextValue instanceof Boolean b && b;
			if (items.isEmpty()) {
				hasNext = false;
			}
			page++;
		}
	}

	private byte[] executeRequest(URI uri, SourceMethod method, SourceModel model, Map<String, String> authHeaders,
			FetchContext context) {
		if (method == SourceMethod.POST) {
			String body = buildPostBody(model, context);
			return restClient.post()
					.uri(uri)
					.headers(headers -> authHeaders.forEach(headers::add))
					.contentType(MediaType.APPLICATION_JSON)
					.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
					.body(body)
					.retrieve()
					.body(byte[].class);
		}
		return restClient.get()
				.uri(uri)
				.headers(headers -> authHeaders.forEach(headers::add))
				.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.retrieve()
				.body(byte[].class);
	}

	private String buildPostBody(SourceModel model, FetchContext context) {
		String template = model.getRequestBodyTemplate();
		if (template == null || template.isBlank()) {
			return "{}";
		}
		return applyPlaceholders(template, context.watermarkValue());
	}

	private String applyPlaceholders(String input, String watermark) {
		if (input == null) {
			return null;
		}
		return input.replace("{watermark}", watermark == null ? "" : watermark);
	}

	private String stripTrailingSlash(String value) {
		return value == null ? "" : (value.endsWith("/") ? value.substring(0, value.length() - 1) : value);
	}

	private String trimSlashes(String value) {
		if (value == null) {
			return "";
		}
		String trimmed = value;
		while (trimmed.startsWith("/")) {
			trimmed = trimmed.substring(1);
		}
		while (trimmed.endsWith("/")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed;
	}

	private String stringOrNull(Object value) {
		return value == null ? null : String.valueOf(value).trim();
	}

	private Map<String, Object> parseJsonWithEncodingFallback(byte[] bytes) {
		TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {
		};
		for (Charset cs : List.of(StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1)) {
			try {
				return objectMapper.readValue(new String(bytes, cs), typeRef);
			} catch (Exception ex) {
				LOG.debug("Falha ao parsear JSON com charset={}: {}", cs, ex.getMessage());
			}
		}
		throw new IllegalStateException("Nao foi possivel decodificar o JSON da resposta (testado UTF-8 e Latin-1)");
	}
}
