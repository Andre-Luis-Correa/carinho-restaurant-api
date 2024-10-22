package com.menumaster.restaurant.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.menumaster.restaurant.aiassistant.domain.dto.ChatMessageDTO;
import com.menumaster.restaurant.aiassistant.domain.dto.ChatResponseDTO;
import com.menumaster.restaurant.category.domain.dto.CategoryDTO;
import com.menumaster.restaurant.dish.domain.dto.DishDTO;
import com.menumaster.restaurant.dish.service.DishService;
import com.menumaster.restaurant.ingredient.domain.dto.IngredientDTO;
import com.menumaster.restaurant.measurementunit.domain.dto.MeasurementUnitDTO;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Log4j2
public class GeminiService {

    private final String geminiQuestionUrl;

    @Autowired
    private DishService dishService;

    public GeminiService(@Value("${gemini.api.key}") String apiKey,
                         @Value("${gemini.question.api.url}") String geminiQuestionUrl) {

        this.geminiQuestionUrl = geminiQuestionUrl + apiKey;
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
                .uri(URI.create(geminiQuestionUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonInput))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return extractTextFromJson(response.body());
    }

    public static String extractTextFromJson(String jsonString) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            JsonNode rootNode = objectMapper.readTree(jsonString);

            JsonNode textNode = rootNode
                    .path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text");

            return textNode.asText();
        } catch (Exception e) {
            e.printStackTrace();
            return "Erro ao processar o JSON.";
        }
    }

    public String recognizeIntent(String userMessage) throws IOException, InterruptedException {
        List<String> intents = new ArrayList<>();
        intents.add("GREETING - Se o usuário enviar uma saudação, retorne essa intenção");
        intents.add("PRESENTATION - Se o usuário solicitar sobre o funcionamento do chat, retorne essa intenção");
        intents.add("CREATE_DISH - Se o usuário quiser criar um novo pratoo, retorne essa intenção");
        intents.add("EDIT_DISH - Se o usuário deseja editar as informações de um prato, retorne essa intenção");
        intents.add("DELETE_DISH - Se o usuário deseja remover um prato, retorne essa intenção");
        intents.add("VIEW_SPECIFIC_DISH - Se o usuário deseja visualizar as ifnormações de um prato específico, retorne essa intenção");
        intents.add("VIEW_MENU - Se o usuário estiver interessado em visualizar o cardápio completo, retorne essa intenção");
        intents.add("OTHER - Se não foi possível classificar a intenção do usuário em uma das categorias anteriores, retorne esta intenção");

        StringBuilder prompt = new StringBuilder("Aja como um atendente virtual do Restaurante Carinho, sendo simpático, educado e convidativo enquanto auxilia os clientes. Sua tarefa é identificar a intenção da mensagem enviada pelo usuário. A partir desta frase: [" + userMessage + "], classifique a intenção dela em uma das seguintes intenções: ");

        for(int i = 0; i < intents.size()-1; i++) {
            prompt.append(intents.get(i)).append(", ");
        }

        prompt.append(intents.get(intents.size()-1)).append(". Retorne apenas a intenção sem nenhum outro texto.");

        log.info(prompt);
        return sendRequest(prompt.toString());
    }


    public ChatResponseDTO processMessageBasedOnIntent(ChatMessageDTO chatMessageDTO, String intent) throws IOException, InterruptedException {
        intent = intent.replace(" \n", "");

        return switch (intent) {
            case "GREETING" -> processGreetingMessage(chatMessageDTO);
            case "PRESENTATION" -> processPresentationMessage(chatMessageDTO);
            case "CREATE_DISH" -> processCreateDishMessage(chatMessageDTO);
            case "EDIT_DISH" -> processEditDishMessage(chatMessageDTO);
            case "DELETE_DISH" -> processDeleteDishMessage(chatMessageDTO);
            case "VIEW_SPECIFIC_DISH" -> processViewSpecificDishMessage(chatMessageDTO);
            case "VIEW_MENU" -> processViewMenuMessage(chatMessageDTO);
            case "OTHER" -> processOtherMessage();
            default -> null;
        };

    }

    private ChatResponseDTO processGreetingMessage(ChatMessageDTO chatMessageDTO) {
        return null;
    }

    private ChatResponseDTO processPresentationMessage(ChatMessageDTO chatMessageDTO) {
        return null;
    }

    private ChatResponseDTO processCreateDishMessage(ChatMessageDTO chatMessageDTO) {
        return null;
    }

    private ChatResponseDTO processEditDishMessage(ChatMessageDTO chatMessageDTO) {
        return null;
    }

    private ChatResponseDTO processDeleteDishMessage(ChatMessageDTO chatMessageDTO) {
        return null;
    }

    private ChatResponseDTO processViewSpecificDishMessage(ChatMessageDTO chatMessageDTO) {
        return null;
    }

    private ChatResponseDTO processViewMenuMessage(ChatMessageDTO chatMessageDTO) throws IOException, InterruptedException {
        List<DishDTO> dishDTOList = dishService.list();

        StringBuilder prompt = new StringBuilder("Aja como um atendente virtual do Restaurante Carinho, sendo simpático, educado e convidativo enquanto auxilia os clientes. O cliente solicitou para visualizar os pratos disponíveis no cardápio.");

        if(dishDTOList.isEmpty()) {
            prompt.append(" No entanto, o cardápio está vazio. Escreva uma mensagem pedindo desculpas e avisando que não há pratos no cardápio.");

        } else {
            prompt.append(" Com base nos seguintes pratos, apresente o cardápio ao cliente. Os pratos serão listados a seguir: ");

            for(int i = 0; i < dishDTOList.size()-1; i++) {
                if(dishDTOList.get(i).isAvailable()) {
                    prompt.append("Prato: ").append(dishDTOList.get(i).name()).append(", Preço (R$): ")
                            .append(dishDTOList.get(i).reaisPrice()).append("Descrição: ").append(dishDTOList.get(i).description())
                            .append("; \n");
                }
            }

            prompt.append("Prato: ").append(dishDTOList.get(dishDTOList.size()-1).name()).append(", Preço (R$): ")
                    .append(dishDTOList.get(dishDTOList.size()-1).reaisPrice()).append("Descrição: ").append(dishDTOList.get(dishDTOList.size()-1).description())
                    .append(".");
        }


        String response = sendRequest(prompt.toString());
        return new ChatResponseDTO(response, true);
    }

    private ChatResponseDTO processOtherMessage() throws IOException, InterruptedException {
        String response = sendRequest("Aja como um atendente virtual do Restaurante Carinho, sendo simpático, educado e convidativo enquanto auxilia os clientes. Retorne uma mensagem pedidno desculpas e dizendo que não conseguiu processar a mensagem do usuário.");
        return new ChatResponseDTO(response, false);
    }

}