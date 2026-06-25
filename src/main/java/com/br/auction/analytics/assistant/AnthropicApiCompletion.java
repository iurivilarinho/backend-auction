package com.br.auction.analytics.assistant;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Provedor de IA via <b>API da Anthropic</b> (Claude) — Messages API, autenticada
 * por token {@code x-api-key}. URL base e modelo sao configuraveis; em branco
 * usam os padroes da Anthropic.
 */
@Component
public class AnthropicApiCompletion implements AiProviderClient {

    private static final String DEFAULT_BASE_URL = "https://api.anthropic.com";
    private static final String DEFAULT_MODEL = "claude-sonnet-4-6";
    private static final String API_VERSION = "2023-06-01";

    private final ObjectMapper objectMapper;

    public AnthropicApiCompletion(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public AiProvider provider() {
        return AiProvider.ANTHROPIC_API;
    }

    @Override
    public String complete(AiSettings settings, String systemPrompt, String userPrompt) {
        requireApiKey(settings);
        int timeoutSeconds = settings.getTimeoutSeconds() > 0 ? settings.getTimeoutSeconds() : 120;
        try {
            Map<String, Object> body = Map.of(
                    "model", model(settings),
                    "max_tokens", 4096,
                    "temperature", 0,
                    "system", systemPrompt,
                    "messages", List.of(Map.of("role", "user", "content", userPrompt)));
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl(settings) + "/v1/messages"))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", settings.getApiKey().trim())
                    .header("anthropic-version", API_VERSION)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new AiUnavailableException("Anthropic respondeu HTTP " + response.statusCode() + ": "
                        + briefError(response.body()), null);
            }
            JsonNode text = objectMapper.readTree(response.body()).path("content").path(0).path("text");
            if (text.isMissingNode() || text.asText().isBlank()) {
                throw new AiUnavailableException("Anthropic nao retornou conteudo.", null);
            }
            return text.asText().trim();
        } catch (AiUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new AiUnavailableException("IA indisponivel (Anthropic): " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable(AiSettings settings) {
        if (!settings.hasApiKey()) {
            return false;
        }
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl(settings) + "/v1/models"))
                    .timeout(Duration.ofSeconds(3))
                    .header("x-api-key", settings.getApiKey().trim())
                    .header("anthropic-version", API_VERSION)
                    .GET().build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() < 500;
        } catch (Exception e) {
            return false;
        }
    }

    private void requireApiKey(AiSettings settings) {
        if (!settings.hasApiKey()) {
            throw new AiUnavailableException("Configure o token da Anthropic em BI / IA -> Configuracoes de IA.", null);
        }
    }

    private String baseUrl(AiSettings settings) {
        String baseUrl = settings.getBaseUrl();
        return (baseUrl == null || baseUrl.isBlank() ? DEFAULT_BASE_URL : baseUrl.trim()).replaceAll("/+$", "");
    }

    private String model(AiSettings settings) {
        String model = settings.getModel();
        return model == null || model.isBlank() ? DEFAULT_MODEL : model.trim();
    }

    private String briefError(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        return body.length() > 300 ? body.substring(0, 300) : body;
    }
}
