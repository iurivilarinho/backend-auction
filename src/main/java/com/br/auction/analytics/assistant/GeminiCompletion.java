package com.br.auction.analytics.assistant;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Provedor de IA via <b>API do Google Gemini</b> (Google AI Studio) —
 * {@code generateContent}, autenticada por token na query {@code key}. URL base
 * e modelo sao configuraveis; em branco usam os padroes do Google.
 */
@Component
public class GeminiCompletion implements AiProviderClient {

    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String DEFAULT_MODEL = "gemini-2.0-flash";

    private final ObjectMapper objectMapper;

    public GeminiCompletion(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public AiProvider provider() {
        return AiProvider.GEMINI;
    }

    @Override
    public String complete(AiSettings settings, String systemPrompt, String userPrompt) {
        requireApiKey(settings);
        int timeoutSeconds = settings.getTimeoutSeconds() > 0 ? settings.getTimeoutSeconds() : 120;
        try {
            Map<String, Object> body = Map.of(
                    "system_instruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                    "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userPrompt)))),
                    "generationConfig", Map.of("temperature", 0));
            String url = baseUrl(settings) + "/v1beta/models/" + model(settings) + ":generateContent?key="
                    + URLEncoder.encode(settings.getApiKey().trim(), StandardCharsets.UTF_8);
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new AiUnavailableException("Gemini respondeu HTTP " + response.statusCode() + ": "
                        + briefError(response.body()), null);
            }
            JsonNode text = objectMapper.readTree(response.body())
                    .path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (text.isMissingNode() || text.asText().isBlank()) {
                throw new AiUnavailableException("Gemini nao retornou conteudo.", null);
            }
            return text.asText().trim();
        } catch (AiUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new AiUnavailableException("IA indisponivel (Gemini): " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable(AiSettings settings) {
        if (!settings.hasApiKey()) {
            return false;
        }
        try {
            String url = baseUrl(settings) + "/v1beta/models?key="
                    + URLEncoder.encode(settings.getApiKey().trim(), StandardCharsets.UTF_8);
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(3)).GET().build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() < 500;
        } catch (Exception e) {
            return false;
        }
    }

    private void requireApiKey(AiSettings settings) {
        if (!settings.hasApiKey()) {
            throw new AiUnavailableException("Configure o token do Gemini em BI / IA -> Configuracoes de IA.", null);
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
