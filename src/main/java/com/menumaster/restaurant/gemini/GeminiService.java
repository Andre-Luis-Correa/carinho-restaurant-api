package com.menumaster.restaurant.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.menumaster.restaurant.aiassistant.domain.dto.ChatMessageDTO;
import com.menumaster.restaurant.aiassistant.domain.dto.ChatResponseDTO;
import com.menumaster.restaurant.category.domain.dto.CategoryDTO;
import com.menumaster.restaurant.category.domain.model.Category;
import com.menumaster.restaurant.category.service.CategoryService;
import com.menumaster.restaurant.dish.domain.dto.DishDTO;
import com.menumaster.restaurant.dish.domain.model.Dish;
import com.menumaster.restaurant.dish.service.DishService;
import com.menumaster.restaurant.ingredient.domain.dto.IngredientDTO;
import com.menumaster.restaurant.ingredient.service.IngredientService;
import com.menumaster.restaurant.measurementunit.domain.dto.MeasurementUnitDTO;
import com.menumaster.restaurant.measurementunit.service.MeasurementUnitService;
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

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private MeasurementUnitService measurementUnitService;

    @Autowired
    private IngredientService ingredientService;

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
            case "PRESENTATION" -> processPresentationMessage();
            case "CREATE_DISH" -> processCreateDishMessage(chatMessageDTO);
            case "EDIT_DISH" -> processEditDishMessage(chatMessageDTO);
            case "DELETE_DISH" -> processDeleteDishMessage(chatMessageDTO);
            case "VIEW_SPECIFIC_DISH" -> processViewSpecificDishMessage(chatMessageDTO);
            case "VIEW_MENU" -> processViewMenuMessage();
            case "OTHER" -> processOtherMessage();
            default -> null;
        };

    }

    private ChatResponseDTO processGreetingMessage(ChatMessageDTO chatMessageDTO) throws IOException, InterruptedException {
        String prompt = "Aja como um atendente virtual do Restaurante Carinho, seja simpático e educado. O cliente enviou uma saudação: [" + chatMessageDTO.userMessage() + "]. Responda de maneira amigável e acolhedora.";

        String response = sendRequest(prompt);

        return new ChatResponseDTO(response, true);
    }

    private ChatResponseDTO processPresentationMessage() throws IOException, InterruptedException {
        String prompt = "Aja como um atendente virtual do Restaurante Carinho. O cliente quer saber mais sobre o restaurante. Explique que o restaurante tem pratos deliciosos no cardápio e que você pode ajudar o cliente a navegar pelos sabores do restaurante!";

        String response = sendRequest(prompt);

        return new ChatResponseDTO(response, true);
    }

    private ChatResponseDTO processCreateDishMessage(ChatMessageDTO chatMessageDTO) throws IOException, InterruptedException {
        if (!chatMessageDTO.userRole().equals("ROLE_ADMINISTRATOR")) {
            return new ChatResponseDTO("Desculpe, mas você não possui a autorização necessária para acessar a funcionalidade de criação de pratos em nosso cardápio. Que tal dar uma olhadinha no cardápio?", false);
        }

        String messagePrompt = """
            Aja como um atendente virtual do Restaurante Carinho, sendo simpático, educado e convidativo enquanto auxilia os clientes.
            O cliente solicitou a criação de um prato a partir da seguinte mensagem:
        """ + chatMessageDTO.userMessage();

        String jsonAndRulesPrompt = """
            Sua resposta deve ser um JSON no formato abaixo, contendo as informações do prato extraídas da mensagem do usuário:
            
            {
              "name": "nome do prato informado pelo usuário",
              "description": "Descrição do prato informada pelo usuário",
              "reaisPrice": valor em reais (R$) do prato informado pelo usuário,
              "pointsPrice": Este valor você pode calcular automaticamente, sabendo que 1 ponto corresponde a R$ 0,10,
              "reaisCostValue": Este é o valor de custo real informado pelo usuário,
              "isAvailable": insira sempre o valor true,
              "categoryId": insira o id da categoria do prato informado pelo usuário baseado no id das categorias pré cadastradas,
              "dishIngredientFormDTOList": [
                {
                  "ingredientId": informe o id do ingrediente informado pelo usuário baseado no id dos ingredientes pré cadastrados,
                  "quantity": informe a quantidade do ingrediente informado pelo usuário,
                  "measurementUnitId": informe o id da unidade de medida do ingrediente para esse prato, baseado no id das unidades de medidas pré cadastradas
                }
              ]
            }
            
            Algumas regras importantes:
            
            1. Todos os campos devem ser preenchidos. Se algum campo não for informado, retorne uma mensagem explicando quais dados estão faltando e que o prato não pode ser criado, não retorne nenhuma estrutura de json nesse caso.
            
            2. Verifique se a categoria, os ingredientes e as unidades de medida informados pelo usuário são válidos comparando-os com os dados pré-cadastrados. Se algum dado estiver incorreto ou não estiver pré-cadastrado, informe exatamente qual dado é inválido e peça desculpas, não retorne nenhuma estrutura de json nesse caso.
            
            3. Cada ingrediente informado deve ter uma quantidade e uma unidade de medida associada. Caso contrário, informe que é necessário fornecer a quantidade e a unidade de medida para cada ingrediente, não retorne nenhuma estrutura de json nesse caso.
            
            4. Se houver mais de uma categoria, informe que apenas uma categoria pode ser atribuída ao prato, não retorne nenhuma estrutura de json nesse caso.
            
            5. Se todos os dados forem válidos e informados pelo usuário, retorne apenas o JSON, sem mensagens adicionais.
            
            6. Se algum erro ocorrer ou alguma regra não for respeitada, retorne apenas uma mensagem explicando os motivos e não retorne nenhuma estrutura de json nesse caso.
        """;

        // Carregar dados existentes de categorias, unidades de medida e ingredientes
        List<CategoryDTO> categoryDTOList = categoryService.list();
        List<MeasurementUnitDTO> measurementUnitDTOList = measurementUnitService.list();
        List<IngredientDTO> ingredientDTOList = ingredientService.list();

        // Gerar a lista de dados cadastrados
        StringBuilder registeredDataPrompt = new StringBuilder("Apenas os dados pré-cadastrados para categoria, unidade de medida e ingrediente podem ser informados pelo usuário. Os dados ppré-cadastrados são os seguintes: \n");

        for (CategoryDTO categoryDTO : categoryDTOList) {
            registeredDataPrompt.append("Categoria -> Id:  " + categoryDTO.id() + " Nome: " + categoryDTO.name() + "\n");
        }
        for (MeasurementUnitDTO measurementUnitDTO : measurementUnitDTOList) {
            registeredDataPrompt.append("Unidade de Medida -> Id: " + measurementUnitDTO.id() + " Nome: " + measurementUnitDTO.name() + " Sigla: " + measurementUnitDTO.acronym() + "\n");
        }
        for (IngredientDTO ingredientDTO : ingredientDTOList) {
            registeredDataPrompt.append("Ingrediente -> Id: " + ingredientDTO.id() + ingredientDTO.name() + "\n");
        }

        // Construir o prompt completo com as regras, dados existentes e mensagem do usuário
        String prompt = messagePrompt + jsonAndRulesPrompt + registeredDataPrompt;
        log.info(prompt);

        // Enviar o prompt ao Gemini e retornar a resposta
        return new ChatResponseDTO(sendRequest(prompt), true);
    }


    private ChatResponseDTO processEditDishMessage(ChatMessageDTO chatMessageDTO) {
        return null;
    }

    private ChatResponseDTO processDeleteDishMessage(ChatMessageDTO chatMessageDTO) throws IOException, InterruptedException {
        String prompt1 = "A partir da seguinte frase: [" +
                chatMessageDTO.userMessage() +
                "], retorne apenas os nomes dos pratos do cardápio que o usuário deseja remover. " +
                "Cada nome deve ser separado por um ponto e vírgula [;], " +
                "caso haja apenas o nome de um prato, retorne apenas o nome dele sem [;]";

        String dishName = sendRequest(prompt1);
        String[] dishNameList = dishName.split(";");

        if(dishNameList.length > 1) {
            String sorryPrompt = "Escreva uma mensagem simples e curta de desculpa ao cliente, informando que não é possível remover do cardápio mais de um prato por vez.";
            return new ChatResponseDTO(sendRequest(sorryPrompt),false);
        }

        String trimmedDish = dishNameList[0].trim();
        Dish dishToBeRemoved = dishService.findFirstByNameContainingIgnoreCase(trimmedDish);
        if(dishToBeRemoved != null) {
            dishService.delete(dishToBeRemoved);
            String successfulRemovalPromt = "Escreva uma mensagem simples e curta, informando ao cliente que o prato " + dishToBeRemoved.getName() +
                    " foi removido do cardápio com sucesso.";
            return new ChatResponseDTO(sendRequest(successfulRemovalPromt), true);
        }

        String unsuccessfulRemovalPromt = "Escreva uma mensagem simples e curta, informando ao cliente que não foi possível escluir o prato " + trimmedDish +
                ", pois não foi encontrado as informações referentes a esse prato no cardápio.";
        return new ChatResponseDTO(sendRequest(unsuccessfulRemovalPromt), true);
    }

    private ChatResponseDTO processViewSpecificDishMessage(ChatMessageDTO chatMessageDTO) throws IOException, InterruptedException {
        String prompt1 = "A partir da seguinte frase: [" + chatMessageDTO.userMessage() +
                "], retorne apenas os nomes dos pratos do cardápio que o usuário deseja visualizar. " +
                "Cada nome deve ser separado por um ponto e vírgula [;], " +
                "caso haja apenas o nome de um prato, retorne apenas o nome dele sem [;]";

        String dishName = sendRequest(prompt1);
        log.info(dishName);
        String[] dishNameList = dishName.split(";");

        List<Dish> foundDishes = new ArrayList<>();
        List<String> notFoundDishes = new ArrayList<>();

        for (String dish : dishNameList) {
            log.info("Nome do prato: " + dish);
            String trimmedDish = dish.trim();
            log.info(trimmedDish);
            List<Dish> dishes = dishService.findByNameContainingIgnoreCase(trimmedDish);

            if (!dishes.isEmpty()) {
                foundDishes.addAll(dishes);
            } else {
                notFoundDishes.add(dish);
            }
        }

        StringBuilder prompt2 = new StringBuilder("Aja como um atendente virtual do Restaurante Carinho, sendo simpático, educado e convidativo enquanto auxilia os clientes, não é necessário cumprimentá-los. O cliente solicitou para visualizar informações de alguns pratos específicos do cardápio: \n");

        if(foundDishes.isEmpty()) {
            prompt2.append("No entanto, não foi possível encontrar nenhum prato solicitado pelo cliente, peça desculpas e diga que não foi possível encontrar as informações dos pratos solicitados.");
        } else {
            prompt2.append("Sendo assim, apresente ao cliente os seguintes pratos: \n");
            for(Dish dish : foundDishes) {
                prompt2.append("Prato: " + dish.getName() + ", Descrição: " + dish.getDescription() + ", Preço (R$): " + dish.getReaisPrice() + "\n");
            }
        }

        if(!notFoundDishes.isEmpty()) {
            prompt2.append("Além disso, peça desculpas e diga que não conseguiu encontrar informações a respeito dos seguintes pratos: ");
            for(String dish : notFoundDishes) {
                prompt2.append("Prato: " + dish + "\n");
            }
        }
        log.info(prompt2);
        return new ChatResponseDTO(sendRequest(prompt2.toString()), true);
    }

    private ChatResponseDTO processViewMenuMessage() throws IOException, InterruptedException {
        List<DishDTO> dishDTOList = dishService.list();

        StringBuilder prompt = new StringBuilder("Aja como um atendente virtual do Restaurante Carinho, sendo simpático, educado e convidativo enquanto auxilia os clientes. O cliente solicitou para visualizar os pratos disponíveis no cardápio.");

        if(dishDTOList.isEmpty()) {
            prompt.append(" No entanto, o cardápio está vazio. Escreva uma mensagem pedindo desculpas e avisando que não há pratos no cardápio.");

        } else {
            prompt.append(" Com base nos seguintes pratos, apresente o cardápio ao cliente. Os pratos serão listados a seguir: ");

            for(DishDTO dishDTO : dishDTOList) {
                if(dishDTO.isAvailable()) {
                    prompt.append("Prato: " + dishDTO.name() + ", Descrição: " + dishDTO.description() + ", Preço (R$): " + dishDTO.reaisPrice() + "\n");
                }
            }
        }

        String response = sendRequest(prompt.toString());
        return new ChatResponseDTO(response, true);
    }

    private ChatResponseDTO processOtherMessage() throws IOException, InterruptedException {
        String prompt = """
                "Aja como um atendente virtual do Restaurante Carinho, sendo simpático, 
                educado e convidativo enquanto auxilia os clientes. Retorne uma mensagem 
                pedindo desculpas e dizendo que não conseguiu processar a mensagem do usuário."
                """;

        String response = sendRequest(prompt);
        return new ChatResponseDTO(response, false);
    }

}