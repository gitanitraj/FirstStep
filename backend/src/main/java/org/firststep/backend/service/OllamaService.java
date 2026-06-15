package org.firststep.backend.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OllamaService {

    private final ObjectMapper mapper = new ObjectMapper();

    private final String apiUrl;
    private final String model;

    private final HttpClient httpClient;

    public OllamaService(
            @Value("${ollama.api.url}") String apiUrl,
            @Value("${ollama.model}") String model
    ) {
        this.apiUrl = apiUrl;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Calls Ollama /api/generate and returns the plain text response.
     */
    public String generate(String prompt, double temperature) throws IOException, InterruptedException {
        Map<String, Object> body = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false,
                "temperature", temperature
        );

        String json = mapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Ollama call failed: status=" + response.statusCode() + ", body=" + response.body());
        }

        JsonNode root = mapper.readTree(response.body());

        // Ollama /api/generate returns { "response": "..." , ... }
        JsonNode responseNode = root.get("response");
        return responseNode == null ? "" : responseNode.asText();
    }
}

