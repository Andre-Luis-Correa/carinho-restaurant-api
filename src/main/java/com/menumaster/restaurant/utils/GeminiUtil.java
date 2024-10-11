package com.menumaster.restaurant.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Component
public class GeminiUtil {

    private final String url;

    public GeminiUtil(@Value("${gemini.api.key}") String apiKey,
                      @Value("${gemini.api.url}") String url) {
        this.url = url + apiKey;
    }

    public String sendRequest(String prompt) throws IOException, InterruptedException {
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                )
        );

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonInput = objectMapper.writeValueAsString(requestBody);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonInput))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return extractTextFromJson(response.body());
    }

    public static String extractTextFromJson(String jsonString) {
        try {
            // Cria um ObjectMapper para manipular o JSON
            ObjectMapper objectMapper = new ObjectMapper();

            // Converte a string JSON para um objeto JsonNode
            JsonNode rootNode = objectMapper.readTree(jsonString);

            // Navega até o campo "text" dentro da estrutura do JSON
            JsonNode textNode = rootNode
                    .path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text");

            // Retorna o texto extraído ou uma mensagem se não encontrado
            return textNode.asText();
        } catch (Exception e) {
            e.printStackTrace();
            return "Erro ao processar o JSON.";
        }
    }
}