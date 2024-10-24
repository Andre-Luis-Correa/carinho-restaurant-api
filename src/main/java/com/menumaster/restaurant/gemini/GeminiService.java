package com.menumaster.restaurant.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.menumaster.restaurant.aiassistant.domain.dto.ChatMessageDTO;
import com.menumaster.restaurant.aiassistant.domain.dto.ChatResponseDTO;
import com.menumaster.restaurant.category.domain.dto.CategoryDTO;
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

        List<String> validationErrors = new ArrayList<>();

        String dishName = getDishNameFromUserMessage(chatMessageDTO.userMessage());
        if(dishName != null && dishName.contains("ERROR")) {
            validationErrors.add("O nome do prato não foi informado corretamente");
        }

        String dishDescription = getDishDescriptionFromUserMessage(chatMessageDTO.userMessage());
        if(dishDescription != null && dishDescription.contains("ERROR")) {
            validationErrors.add("A descrição do prato não foi informada corretamente");
        }

        String dishReaisPrice = getDishReaisPriceFromUserMessage(chatMessageDTO.userMessage());
        if(dishReaisPrice != null && dishReaisPrice.contains("ERRO")) {
            validationErrors.add("O preço em reais (R$) do prato não foi informado corretamente.");
        }

        String dishReaisCostPrice = getDishReaisCostPriceFromUserMessage(chatMessageDTO.userMessage());
        if(dishReaisCostPrice != null && dishReaisCostPrice.contains("ERRO")) {
            validationErrors.add("O preço de custo em reais (R$) do prato não foi informado corretamente.");
        }

        String dishIsAvailable = "true";

        String dishCategoryId = getDishCategoryFromUserMessage(chatMessageDTO.userMessage());
        if(dishCategoryId != null && dishCategoryId.contains("ERRO")) {
            validationErrors.add("O categoria do prato não foi informada corretamente.");
        }

        String jsonAndRulesPrompt = """
            {
              "name": "nome do prato informado pelo usuário",
              "description": "Descrição do prato informada pelo usuário",
              "reaisPrice": valor em reais (R$) do prato informado pelo usuário,
              "pointsPrice": Este valor você pode calcular automaticamente, sabendo que 1 ponto corresponde a R$ 0,10,
              "reaisCostValue": Este é o valor de custo real informado pelo usuário,
              "isAvailable": insira sempre o valor true,
              "categoryId": insira o id da categoria do prato informado pelo usuário, desde que exista entre as categorias pré cadastradas,
              "dishIngredientFormDTOList": [
                {
                  "ingredientId": informe o id do ingrediente informado pelo usuário, desde que ela exista entre os ingredientes pré cadastrados,
                  "quantity": informe a quantidade do ingrediente informado pelo usuário,
                  "measurementUnitId": informe o id da unidade de medida informada pelo usuário para o ingrediente, desde que exita entra as unidades de medidas pré cadastradas
                }
              ]
            }
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
            registeredDataPrompt.append("Ingrediente -> Id: " + ingredientDTO.id() + " Nome: " + ingredientDTO.name() + "\n");
        }

        // Enviar o prompt ao Gemini e retornar a resposta
        //return new ChatResponseDTO(sendRequest(prompt), true);
        return new ChatResponseDTO(dishName + "\n" + dishDescription + "\n" + dishReaisPrice + "\n" + dishReaisCostPrice
                + dishCategoryId, false);
    }

    private String getDishNameFromUserMessage(String userMessage) throws IOException, InterruptedException {
        String prompt = "O usuário enviou a seguinte mensagem com a intenção de criar um prato: [" + userMessage + "]." +
                """
                A partir dessa mensagem identifique o nome do prato e se identificado retorne apenas o nome do prato.
                Caso não seja possível identificar o nome do prato retorne apenas a palavra [ERRO].
                
                Exemplos:
                
                [Quero criar um prato com nome farofa] = retorne apenas [farofa]
                [Quero criar o prato canjica] = retorne apenas [canjica].
                [Quero criar um prato] = retorne apenas [ERRO], pois não foi informado o nome.
                """;

        String dishName = sendRequest(prompt);
        log.info("Nome do prato: " + dishName);

        return dishName;
    }

    private String getDishDescriptionFromUserMessage(String userMessage) throws IOException, InterruptedException {
        String prompt = "O usuário enviou a seguinte mensagem com a intenção de criar um prato: [" + userMessage + "]." +
                """
                A partir dessa mensagem identifique a descrição do prato e se identificado retorne apenas a descrição do prato.
                Caso não seja possível identificar a descrição do prato retorne apenas a palavra [ERRO].
                
                Exemplos:
                
                [Quero criar um prato com nome farofa, com a descrição delicioso e suculento] = retorne apenas [delicioso e suculento].
                [Quero criar um prato com nome espetinho, irresístivel sabor do restaurante carinho] = retorne apenas [irresístivel sabor do restaurante carinho].
                [Quero criar o prato canjica, delicioso sabor de sobremesa brasileira] = retorne apenas [delicioso sabor de sobremesa brasileira].
                [Quero criar um prato chamado costelão] = retorne apenas a palavra [ERRO], pois não foi informado uma descrição para o prato.
                """;

        String dishDescription = sendRequest(prompt);
        log.info("Descrição do prato: " + dishDescription);

        return dishDescription;
    }

    private String getDishReaisPriceFromUserMessage(String userMessage) throws IOException, InterruptedException {
        String prompt = "O usuário enviou a seguinte mensagem com a intenção de criar um prato: [" + userMessage + "]." +
                """
                A partir dessa mensagem identifique o preço em reais (R$) do prato e se identificado retorne apenas o preço em reais (R$) do prato.
                Caso não seja possível identificar o preço em reais (R$) do prato retorne apenas a palavra [ERRO].
                
                Exemplos:
                
                [Quero criar um prato com nome farofa, com a descrição delicioso e suculento que custe R$ 29,90] = retorne apenas [29.90].
                [Quero criar um prato com nome espetinho, irresístivel sabor do restaurante carinho, custando 29.90 reais] = retorne apenas [29.90].
                [Quero criar o prato canjica, delicioso sabor de sobremesa brasileira, com valor de 29 reais] = retorne apenas [29].
                [Quero criar um prato chamado costelão, com a descrição saboroso e suculento] = retorne apenas a palavra [ERRO], pois não foi informado o preço em reais (R$) para o prato.
                Observação: caso a pessoa tenha digitado um valor númerico incorreto para o preço, por exemplo, 30.9.9; 39,9,9; 35.0000,4, ou outros, retorne apenas [ERRO].
                Além disso, os únicos valores númericos aceitos para o preço seguem o padrão [0-9]*.[0-9][0-9] ou [0-9]*,[0-9][0-9], sempre arredonde para 2 casas decimais.
                """;

        String dishReaisPrice = sendRequest(prompt);
        log.info("Preço em R$ do prato: " + dishReaisPrice);

        return dishReaisPrice;
    }

    private String getDishReaisCostPriceFromUserMessage(String userMessage) throws IOException, InterruptedException {
        String prompt = "O usuário enviou a seguinte mensagem com a intenção de criar um prato: [" + userMessage + "]." +
                """
                A partir dessa mensagem identifique o preço de custo em reais (R$) do prato e se identificado retorne apenas o preço de custo em reais (R$) do prato.
                Caso não seja possível identificar o preço de custo em reais (R$) do prato retorne apenas a palavra [ERRO].
                
                Exemplos:
                
                [Quero criar um prato com nome farofa, com a descrição delicioso e suculento que custe R$ 29,90, com preço de custo de 20 reais.] = retorne apenas [20].
                [Quero criar um prato com nome espetinho, irresístivel sabor do restaurante carinho, custando 29.90 reais, que tem um preço real de R$ 22.00] = retorne apenas [22.00].
                [Quero criar o prato canjica, delicioso sabor de sobremesa brasileira, com valor de 29 reais e preço de custo de R$ 15] = retorne apenas [15].
                [Quero criar um prato chamado costelão, com a descrição saboroso e suculento, com valor de 40 reais] = retorne apenas a palavra [ERRO], pois não foi informado o preço de custo em reais (R$) para o prato.
                Observação: caso a pessoa tenha digitado um valor númerico incorreto para o preço de custo, por exemplo, 30.9.9; 39,9,9; 35.0000,4, ou outros, retorne apenas [ERRO].
                Além disso, os únicos valores númericos aceitos para o preço de custo seguem o padrão [0-9]*.[0-9][0-9] ou [0-9]*,[0-9][0-9], sempre arredonde para 2 casas decimais.
                """;

        String dishReaisCostPrice = sendRequest(prompt);
        log.info("Preço de custo em R$ do prato: " + dishReaisCostPrice);

        return dishReaisCostPrice;
    }

    private String getDishCategoryFromUserMessage(String userMessage) throws IOException, InterruptedException {
        List<CategoryDTO> categoryDTOList = categoryService.list();

        StringBuilder validCategoryPrompt = new StringBuilder("As categorias válidas são: \n");
        for (CategoryDTO categoryDTO : categoryDTOList) {
            validCategoryPrompt.append("Categoria -> Id:  " + categoryDTO.id() + " Nome: " + categoryDTO.name() + "\n");
        }

        String prompt = "O usuário enviou a seguinte mensagem com a intenção de criar um prato: [" + userMessage + "]." +
                """
                A partir dessa mensagem identifique categoria do prato e se identificado retorne apenas o id da categoria válido do prato.
                Caso não seja possível identificar a categoria do prato retorne apenas a palavra [ERRO].
                """
                + validCategoryPrompt +
                """
                Exemplos:
                
                [Quero criar um prato com nome farofa, com a descrição delicioso e suculento que custe R$ 29,90, com preço de custo de 20 reais, na categoria brasil.] = retorne apenas um numero que representa o id da categoria válida.
                [Quero criar um prato com nome espetinho, irresístivel sabor do restaurante carinho, custando 29.90 reais, que tem um preço real de R$ 22.00, na categoria carnes] = retorne apenas um numero que representa o id da categoria válida.
                [Quero criar o prato canjica, delicioso sabor de sobremesa brasileira, com valor de 29 reais e preço de custo de R$ 15, insira-o na categoria sobremesas] = retorne apenas um numero que representa o id da categoria válida.
                [Quero criar um prato chamado costelão, com a descrição saboroso e suculento, com valor de 40 reais e preço de custo de 20 reais.] = retorne apenas a palavra [ERRO], pois não foi informado a categoria para o prato.
                Observação: caso a categoria informada não esteja entre as categorias válidas, retorne [ERRO].
                Além disso, caso o usuário informe mais de uma categoria, retorne apenas [ERRO].
                """;

        String dishCategory = sendRequest(prompt);
        log.info(prompt);
        log.info("Categoria do prato: " + dishCategory);

        return dishCategory;
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