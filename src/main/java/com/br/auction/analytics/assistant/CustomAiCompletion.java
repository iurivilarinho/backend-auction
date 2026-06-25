package com.br.auction.analytics.assistant;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Provedor "Modelo proprio" — endpoint generico, pensado para um <b>gpt-cli</b>
 * local (que dirige ChatGPT/Gemini/Grok/DeepSeek via navegador, sem custo de API),
 * mas tolerante a qualquer servidor simples de chat.
 *
 * <p>Protocolo: {@code POST {baseUrl}/chat} com corpo {@code {"message": <prompt>,
 * "continue": false}} e le a resposta de forma flexivel (campos {@code reply},
 * {@code response}, {@code text}, {@code content} ou no formato OpenAI
 * {@code choices[0].message.content}). Disponibilidade: {@code GET {baseUrl}/status}.
 * URL base vem da configuracao em runtime (sem token).
 */
@Component
public class CustomAiCompletion implements AiProviderClient {

    private static final String DEFAULT_BASE_URL = "http://127.0.0.1:4179";

    private final ObjectMapper objectMapper;

    public CustomAiCompletion(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public AiProvider provider() {
        return AiProvider.CUSTOM;
    }

    @Override
    public String complete(AiSettings settings, String systemPrompt, String userPrompt) {
        String baseUrl = baseUrl(settings);
        int timeoutSeconds = settings.getTimeoutSeconds() > 0 ? settings.getTimeoutSeconds() : 120;
        String message = (systemPrompt == null ? "" : systemPrompt.trim())
                + (systemPrompt != null && !systemPrompt.isBlank() ? "\n\n" : "")
                + (userPrompt == null ? "" : userPrompt.trim());
        try {
            Map<String, Object> body = Map.of("message", message, "continue", false);
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/chat"))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new AiUnavailableException("Modelo proprio respondeu HTTP " + response.statusCode() + ".", null);
            }
            String text = extractReply(response.body());
            if (text == null || text.isBlank()) {
                throw new AiUnavailableException("Modelo proprio nao retornou conteudo.", null);
            }
            return text.trim();
        } catch (AiUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new AiUnavailableException("Assistente de IA indisponivel (modelo proprio): " + e.getMessage(), e);
        }
    }

    /** Le a resposta de forma tolerante: aceita varios formatos de retorno. */
    private String extractReply(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            for (String field : new String[] { "reply", "response", "text", "content", "output", "answer" }) {
                JsonNode v = root.path(field);
                if (v.isTextual() && !v.asText().isBlank()) {
                    return v.asText();
                }
            }
            // formato estilo OpenAI: choices[0].message.content
            JsonNode openai = root.path("choices").path(0).path("message").path("content");
            if (openai.isTextual() && !openai.asText().isBlank()) {
                return openai.asText();
            }
            // ultimo recurso: corpo cru (alguns endpoints devolvem texto puro)
            return responseBody != null && !responseBody.isBlank() && !responseBody.trim().startsWith("{")
                    ? responseBody
                    : null;
        } catch (Exception parseFailure) {
            // nao era JSON — pode ser texto puro
            return responseBody;
        }
    }

    /** Sonda {@code GET {baseUrl}/status} com timeout curto — sem inferencia. */
    @Override
    public boolean isAvailable(AiSettings settings) {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl(settings) + "/status"))
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
}
