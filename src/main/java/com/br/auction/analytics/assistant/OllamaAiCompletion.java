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
 * Provedor de IA via <b>Ollama</b> local/auto-hospedado, por HTTP direto
 * ({@code POST /api/chat}). Le a URL base e o modelo da configuracao em runtime,
 * entao apontar para outro host/modelo nao exige reiniciar a aplicacao.
 */
@Component
public class OllamaAiCompletion implements AiProviderClient {

    private static final String DEFAULT_BASE_URL = "http://localhost:11434";
    private static final String DEFAULT_MODEL = "llama3";

    private final ObjectMapper objectMapper;

    public OllamaAiCompletion(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public AiProvider provider() {
        return AiProvider.OLLAMA;
    }

    @Override
    public String complete(AiSettings settings, String systemPrompt, String userPrompt) {
        String baseUrl = baseUrl(settings);
        String model = model(settings);
        int timeoutSeconds = settings.getTimeoutSeconds() > 0 ? settings.getTimeoutSeconds() : 120;
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "stream", false,
                    "options", Map.of("temperature", 0),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)));
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/chat"))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new AiUnavailableException("Ollama respondeu HTTP " + response.statusCode() + ".", null);
            }
            JsonNode text = objectMapper.readTree(response.body()).path("message").path("content");
            if (text.isMissingNode() || text.asText().isBlank()) {
                throw new AiUnavailableException("Ollama nao retornou conteudo.", null);
            }
            return text.asText().trim();
        } catch (AiUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new AiUnavailableException("Assistente de IA indisponivel (Ollama): " + e.getMessage(), e);
        }
    }

    /** Sonda o endpoint do Ollama (/api/tags) com timeout curto — sem inferencia. */
    @Override
    public boolean isAvailable(AiSettings settings) {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl(settings) + "/api/tags"))
                    .timeout(Duration.ofSeconds(3)).GET().build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() < 500;
        } catch (Exception e) {
            return false;
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
}
