package com.menumaster.restaurant.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.menumaster.restaurant.aiassistant.domain.dto.ChatMessageDTO;
import com.menumaster.restaurant.aiassistant.domain.dto.ChatResponseDTO;
import com.menumaster.restaurant.category.domain.dto.CategoryDTO;
import com.menumaster.restaurant.category.service.CategoryService;
import com.menumaster.restaurant.dish.domain.dto.DishDTO;
import com.menumaster.restaurant.dish.domain.dto.DishFormDTO;
import com.menumaster.restaurant.dish.domain.dto.DishIngredientFormDTO;
import com.menumaster.restaurant.dish.domain.model.Dish;
import com.menumaster.restaurant.dish.domain.model.DishIngredient;
import com.menumaster.restaurant.dish.service.DishService;
import com.menumaster.restaurant.exception.type.ExtractingJsonDataException;
import com.menumaster.restaurant.ingredient.domain.dto.IngredientDTO;
import com.menumaster.restaurant.ingredient.domain.model.Ingredient;
import com.menumaster.restaurant.ingredient.service.IngredientService;
import com.menumaster.restaurant.measurementunit.domain.dto.MeasurementUnitDTO;
import com.menumaster.restaurant.measurementunit.domain.model.MeasurementUnit;
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

    public String getInitialCommand(String userMessage) {
        return String.format("""
               Atue como um assistente virtual de um restaurante, o Restaurante Carinho. 
               Você deve forneceer respostas amigáveis, criativas e educadas. 
               Apenas responda perguntas referentes ao restaurante Carinho.
                                    
               O Usuário enviou a seguinte mensagem: "%s" \n
               """, userMessage);
    }

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
            throw new ExtractingJsonDataException("text");
        }
    }

    public String recognizeIntent(String userMessage) throws IOException, InterruptedException {
        List<String> intents = new ArrayList<>();
        intents.add("1. Se o cliente solicitar sobre seu funcionamento ou sobre as suas funções, retorne apenas uma mensagem explicando que você pode ajudá-lo a explorar os pratos do cardápio.");
        intents.add("2. Se o cliente enviar uma saudação, retorne apenas uma mensagem de saudação ao cliente");
        intents.add("3. Se o cliente quiser criar um novo pratoo, retorne apenas a palavra entre colchetes [CREATE_DISH].");
        intents.add("4. Se o cliente deseja remover um prato, retorne apenas a palavra entre colchetes [DELETE_DISH].");
        intents.add("5. Se o cliente deseja visualizar as informações de um prato específico, retorne apenas a palavra entre colchetes [VIEW_SPECIFIC_DISH].");
        intents.add("6. Se o cliente estiver interessado em visualizar o cardápio completo, retorne apenas a palavra entre colchetes [VIEW_MENU].");
        intents.add("7. Se a mensagem do cliente não se encaixou em nenhuma das regras anteriores, então retorne apenas uma mensagem pedindo desculpas e explicando que não conseguiu interpretar a mensagem do cliente.");

        StringBuilder prompt = new StringBuilder(getInitialCommand(userMessage) + "Sua tarefa é retornar uma mensagem a partir da mensagem do cliente, além disso, a mensagem deve ser escrita com base nas seguintes regras: ");
        for(int i = 0; i < intents.size()-1; i++) {
            prompt.append(intents.get(i)).append("\n");
        }

        log.info(prompt);
        return sendRequest(prompt.toString());
    }


    public ChatResponseDTO processMessageBasedOnIntent(ChatMessageDTO chatMessageDTO, String intent) throws IOException, InterruptedException {
        if(intent.contains("CREATE_DISH")) intent = "CREATE_DISH";
        if(intent.contains("DELETE_DISH")) intent = "DELETE_DISH";
        if(intent.contains("VIEW_SPECIFIC_DISH")) intent = "VIEW_SPECIFIC_DISH";
        if(intent.contains("VIEW_MENU")) intent = "VIEW_MENU";

        return switch (intent) {
            case "CREATE_DISH" -> processCreateDishMessage(chatMessageDTO);
            case "DELETE_DISH" -> processDeleteDishMessage(chatMessageDTO);
            case "VIEW_SPECIFIC_DISH" -> processViewSpecificDishMessage(chatMessageDTO);
            case "VIEW_MENU" -> processViewMenuMessage();
            default -> processUserMessage(intent);
        };

    }

    public ChatResponseDTO processCreateDishMessage(ChatMessageDTO chatMessageDTO) throws IOException, InterruptedException {
        List<CategoryDTO> categories = categoryService.list();
        List<IngredientDTO> ingredients = ingredientService.list();
        List<MeasurementUnitDTO> units = measurementUnitService.list();

        String prompt = createDishCreationPrompt(chatMessageDTO.userMessage(), categories, ingredients, units);
        log.info(prompt);
        String responseJson = sendRequest(prompt);
        log.info(responseJson);
        if(responseJson.contains("[ERRO]")) {
            responseJson = responseJson.replace("[ERRO]", "");
            return new ChatResponseDTO(responseJson, false);
        }
        DishFormDTO dishFormDTO = parseDishFormDTO(responseJson);
        return new ChatResponseDTO(dishFormDTO.toString(), true);
    }

    private String createDishCreationPrompt(String userMessage, List<CategoryDTO> categories, List<IngredientDTO> ingredients, List<MeasurementUnitDTO> units) {
        StringBuilder prompt = new StringBuilder("Atue como um assistente virtual para o Restaurante Carinho.");

        if (!categories.isEmpty()) {
            prompt.append("\n**Categorias Válidas**:\n");
            for (CategoryDTO category : categories) {
                prompt.append("Categoria válida -> Id: ").append(category.id()).append(", com nome: ").append(category.name()).append("\n");
            }
        } else {
            prompt.append("\nNão existe nenhuma categoria válida cadastrada\n");
        }

        if (!ingredients.isEmpty()) {
            prompt.append("\n**Ingredientes Válidos**:\n");
            for (IngredientDTO ingredient : ingredients) {
                prompt.append("Ingrediente válido -> Id: ").append(ingredient.id()).append(", com nome: ").append(ingredient.name()).append("\n");
            }
        } else {
            prompt.append("\nNão existe nenhum ingrediente válido cadastrado\n");
        }

        if (!units.isEmpty()) {
            prompt.append("\n**Unidades de Medida Válidas (Apenas Nomes Completos)**:\n");
            for (MeasurementUnitDTO unit : units) {
                prompt.append("Unidade de Medida válida -> Id: ").append(unit.id()).append(", com nome: ").append(unit.name()).append("\n");
            }
        } else {
            prompt.append("\nNão existe nenhuma unidade de medida válida cadastrada\n");
        }

        prompt.append(" O cliente enviou uma mensagem contendo informações sobre um prato a ser criado: [")
                .append(userMessage).append("]. Extraia as informações necessárias e preencha o JSON com os dados dos DTOs `DishFormDTO` e `DishIngredientFormDTO`.\n\n")
                .append("**Regras e Instruções para Extração e Validação dos Dados**:\n");

        prompt.append("""
    **Regras e Instruções para Extração e Validação dos Dados**:
    
    **Categoria**:
    1. O nome da categoria deve ser mencionado explicitamente pelo cliente na mensagem e ser **exatamente igual a um dos nomes das categorias válidas listadas acima**.
    2. Não associe nenhuma categoria automaticamente se o nome fornecido pelo cliente não corresponder exatamente a um dos nomes válidos.
    3. Se a categoria estiver mencionada apenas como "categoria" sem nome, de forma incompleta, ou diferente dos nomes listados, retorne uma mensagem de erro começando com [ERRO].
    4. Caso o cliente informe mais de uma categoria, também retorne uma mensagem de erro começando com [ERRO].

    **Ingredientes**:
    1. Ingredientes são obrigatórios para a criação de um prato. Cada prato deve incluir pelo menos um ingrediente válido, conforme listado acima.
    2. Cada ingrediente deve ter uma quantidade especificada e uma unidade de medida válida (veja as unidades de medida válidas abaixo).
    3. Se algum ingrediente não for encontrado na lista de ingredientes válidos ou se faltar quantidade ou unidade de medida, retorne uma mensagem de erro começando com [ERRO].

    **Unidade de Medida**:
    1. A unidade de medida para cada ingrediente deve ser informada **exatamente conforme os nomes completos listados acima**. Não aceite siglas, abreviações ou variações.
    2. Caso a unidade de medida informada pelo cliente não corresponda exatamente ao nome completo listado acima, retorne uma mensagem de erro começando com [ERRO].
    
    **Regras Gerais**:
    1. Todos os dados obrigatórios devem estar presentes na mensagem do cliente. Se algum dado estiver ausente, retorne uma mensagem de erro começando com [ERRO], especificando o campo ausente.
    2. O valor do prato em reais (`reaisPrice`) e o valor de custo (`reaisCostValue`) devem ser informados e ter 2 casas decimais. Arredonde valores, se necessário.
    3. Defina sempre `isAvailable` como `true`.

    **IMPORTANTE**:
    - Se todos os dados forem extraídos e validados corretamente, retorne apenas o JSON final, sem texto adicional.
    - Caso algum erro ocorra, a resposta deve começar com [ERRO] e descrever o problema específico.
    - Utilize apenas os dados listados acima para validar categoria, ingredientes e unidades de medida. Qualquer valor não listado deve ser tratado como inválido.
""");

        prompt.append("\nFormato JSON esperado para o DTO:\n")
                .append("""
              {
                "name": "Nome do prato",
                "description": "Descrição do prato",
                "reaisPrice": 29.90,
                "pointsPrice": 299,
                "reaisCostValue": 20.00,
                "image": "URL da imagem",
                "isAvailable": true,
                "categoryId": 1,
                "dishIngredientFormDTOList": [
                  {
                    "ingredientId": 3,
                    "quantity": 0.5,
                    "measurementUnitId": 2
                  }
                ]
              }
          """);

        return prompt.toString();
    }



    private DishFormDTO parseDishFormDTO(String responseJson) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(responseJson);

        String name = rootNode.path("name").asText();
        String description = rootNode.path("description").asText();
        Double reaisPrice = rootNode.path("reaisPrice").asDouble();
        Integer pointsPrice = rootNode.path("pointsPrice").asInt();
        Double reaisCostValue = rootNode.path("reaisCostValue").asDouble();
        String image = rootNode.path("image").asText();
        Long categoryId = rootNode.path("categoryId").asLong();

        List<DishIngredientFormDTO> dishIngredients = new ArrayList<>();
        JsonNode ingredientsNode = rootNode.path("dishIngredientFormDTOList");
        for (JsonNode ingredientNode : ingredientsNode) {
            Long ingredientId = ingredientNode.path("ingredientId").asLong();
            Double quantity = ingredientNode.path("quantity").asDouble();
            Long measurementUnitId = ingredientNode.path("measurementUnitId").asLong();
            dishIngredients.add(new DishIngredientFormDTO(ingredientId, quantity, measurementUnitId));
        }

        return new DishFormDTO(name, description, reaisPrice, pointsPrice, reaisCostValue, image, true, categoryId, dishIngredients);
    }

    private ChatResponseDTO processUserMessage(String intent) {
        return new ChatResponseDTO(intent, true);
    }

    private ChatResponseDTO processDeleteDishMessage(ChatMessageDTO chatMessageDTO) throws IOException, InterruptedException {
        if (!chatMessageDTO.userRole().equals("ROLE_ADMINISTRATOR")) {
            return new ChatResponseDTO("Desculpe, mas você não possui a autorização necessária para acessar a funcionalidade de remoçao de pratos em nosso cardápio. Que tal dar uma olhadinha no cardápio?", false);
        }

//        String prompt1 = "A partir da seguinte frase: [" +
//                chatMessageDTO.userMessage() +
//                "], retorne uma mensagem contendo apenas os nomes dos pratos do cardápio que o usuário deseja remover ou excluir. " +
//                "Cada nome deve ser separado por um ponto e vírgula [;], " +
//                "caso haja apenas o nome de um prato, retorne apenas o nome dele sem [;].\n" +
//                """
//                Por exemplo, se o cliente enviar a mensagem "quero excluir o prato espetinho" retorne uma mensagem contendo
//                apenas "espetinho".
//                Outro exemplo: se o cliente enviar a mensagem "quero excluir o espetinho e o bolo de cenoura, então retorne
//                uma mensagem contento apenas "espetinho;bolo de cenoura".
//                """;
        String prompt1 = "Dada a seguinte frase do cliente: [" + chatMessageDTO.userMessage() + "], identifique e retorne apenas os nomes dos pratos que o cliente deseja remover ou excluir do cardápio. Responda da seguinte forma:\n" +
                "- Caso haja um único prato, retorne somente o nome do prato, sem adições.\n" +
                "- Caso o cliente deseje remover mais de um prato, liste cada nome separado por ponto e vírgula [;].\n\n" +
                """
                Exemplos:
                
                1. Para a mensagem: "quero excluir o prato espetinho", retorne apenas "espetinho".
                2. Para a mensagem: "quero excluir o espetinho e o bolo de cenoura", retorne "espetinho;bolo de cenoura".
                
                Lembre-se de retornar exatamente conforme o solicitado, sem incluir informações adicionais.
                """;
        log.info(prompt1);
        String dishName = sendRequest(prompt1);
        String[] dishNameList = dishName.split(";");
        log.info(dishName);
        if(dishNameList.length > 1) {
            String sorryPrompt = "Não cumprimente o cliente e escreva uma mensagem simples e curta de desculpa ao cliente, informando que não é possível remover do cardápio mais de um prato por vez.";
            return new ChatResponseDTO(sendRequest(sorryPrompt),false);
        }

        String trimmedDish = dishNameList[0].trim();
        Dish dishToBeRemoved = dishService.findFirstByNameContainingIgnoreCase(trimmedDish);
        if(dishToBeRemoved != null) {
            dishService.delete(dishToBeRemoved);
            String successfulRemovalPromt = "Não cumprimente o cliente e escreva uma mensagem simples e curta, informando ao cliente que o prato " + dishToBeRemoved.getName() +
                    " foi removido do cardápio com sucesso.";
            return new ChatResponseDTO(sendRequest(successfulRemovalPromt), true);
        }

        String unsuccessfulRemovalPromt = "Não cumprimente o cliente e escreva uma mensagem simples e curta, informando ao cliente que não foi possível excluir o prato " + trimmedDish +
                ", pois não foi encontrado as informações referentes a esse prato no cardápio.";
        return new ChatResponseDTO(sendRequest(unsuccessfulRemovalPromt), true);
    }

    private ChatResponseDTO processViewSpecificDishMessage(ChatMessageDTO chatMessageDTO) throws IOException, InterruptedException {
        String promptGetDishNames = "A partir da seguinte frase: ["
                                    + chatMessageDTO.userMessage() +
                                    """
                                    ], retorne apenas os nomes dos pratos do cardápio que o usuário deseja visualizar.
                                    Cada nome deve ser separado por um ponto e vírgula [;], caso haja apenas o nome de um prato, 
                                    retorne apenas o nome dele sem [;]
                                    """;

        String dishName = sendRequest(promptGetDishNames);
        String[] dishNameList = dishName.split(";");

        List<Dish> foundDishes = new ArrayList<>();
        List<String> notFoundDishes = new ArrayList<>();

        for (String dish : dishNameList) {
            String trimmedDish = dish.trim();

            List<Dish> dishes = dishService.findByNameContainingIgnoreCase(trimmedDish);

            if (!dishes.isEmpty()) foundDishes.addAll(dishes);
            else notFoundDishes.add(dish);
        }

        StringBuilder promptIntroduceSpecificDishes = new StringBuilder("Não cumprimente o cliente. O cliente solicitou para visualizar informações de alguns pratos específicos do cardápio. ");

        if(foundDishes.isEmpty()) {
            promptIntroduceSpecificDishes.append("No entanto, não foi possível encontrar nenhum prato solicitado pelo cliente, peça desculpas e diga que não foi possível encontrar as informações dos pratos solicitados.");
        } else {
            promptIntroduceSpecificDishes.append("Sendo assim, apresente ao cliente de maneira amigável e divertida as informações dos seguintes pratos: \n");
            int i = 1;
            for(Dish dish : foundDishes) {
                promptIntroduceSpecificDishes.append(i++).append("Prato: ").append(dish.getName()).append(", Descrição: ").append(dish.getDescription()).append(", Preço (R$): ").append(dish.getReaisPrice()).append("\n");
            }
        }

        if(!notFoundDishes.isEmpty()) {
            promptIntroduceSpecificDishes.append("Além disso, peça desculpas e diga que não conseguiu encontrar informações a respeito dos seguintes pratos: ");
            int i = 1;
            for(String dish : notFoundDishes) {
                promptIntroduceSpecificDishes.append(i++).append("Prato: ").append(dish).append("\n");
            }
        }

        return new ChatResponseDTO(sendRequest(promptIntroduceSpecificDishes.toString()), true);
    }

    private ChatResponseDTO processViewMenuMessage() throws IOException, InterruptedException {
        List<DishDTO> dishDTOList = dishService.list();
        StringBuilder prompt = new StringBuilder("Não cumprimente o cliente. O cliente solicitou para visualizar os pratos disponíveis no cardápio. ");

        if(dishDTOList.isEmpty()) {
            prompt.append("No entanto, o cardápio está vazio. Escreva uma mensagem pedindo desculpas e avisando que não há pratos no cardápio.");
        } else {
            prompt.append("Com base nos seguintes pratos, apresente o cardápio ao cliente de forma criativa, amigável e educada. Os pratos serão listados a seguir: ");

            for(DishDTO dishDTO : dishDTOList) {
                if(dishDTO.isAvailable()) {
                    prompt.append("Prato: " + dishDTO.name() + ", Descrição: " + dishDTO.description() + ", Preço (R$): " + dishDTO.reaisPrice() + "\n");
                }
            }
        }

        String response = sendRequest(prompt.toString());
        return new ChatResponseDTO(response, true);
    }

//    private ChatResponseDTO processCreateDishMessage(ChatMessageDTO chatMessageDTO) throws IOException, InterruptedException {
//        if (!chatMessageDTO.userRole().equals("ROLE_ADMINISTRATOR")) {
//            return new ChatResponseDTO("Desculpe, mas você não possui a autorização necessária para acessar a funcionalidade de criação de pratos em nosso cardápio. Que tal dar uma olhadinha no cardápio?", false);
//        }
//
//        List<String> validationErrors = new ArrayList<>();
//
//        String dishName = getDishNameFromUserMessage(chatMessageDTO.userMessage());
//        if(dishName != null && dishName.contains("ERROR")) {
//            validationErrors.add("O nome do prato não foi informado corretamente");
//        }
//
//        String dishDescription = getDishDescriptionFromUserMessage(chatMessageDTO.userMessage());
//        if(dishDescription != null && dishDescription.contains("ERROR")) {
//            validationErrors.add("A descrição do prato não foi informada corretamente");
//        }
//
//        String dishReaisPrice = getDishReaisPriceFromUserMessage(chatMessageDTO.userMessage());
//        if(dishReaisPrice != null && dishReaisPrice.contains("ERRO")) {
//            validationErrors.add("O preço em reais (R$) do prato não foi informado corretamente.");
//        }
//
//        String dishReaisCostPrice = getDishReaisCostPriceFromUserMessage(chatMessageDTO.userMessage());
//        if(dishReaisCostPrice != null && dishReaisCostPrice.contains("ERRO")) {
//            validationErrors.add("O preço de custo em reais (R$) do prato não foi informado corretamente.");
//        }
//
//        String dishIsAvailable = "true";
//
//        String dishCategoryId = getDishCategoryFromUserMessage(chatMessageDTO.userMessage());
//        if(dishCategoryId != null && dishCategoryId.contains("ERRO")) {
//            validationErrors.add("O categoria do prato não foi informada corretamente.");
//        }
//
//        String jsonAndRulesPrompt = """
//            {
//              "name": "nome do prato informado pelo usuário",
//              "description": "Descrição do prato informada pelo usuário",
//              "reaisPrice": valor em reais (R$) do prato informado pelo usuário,
//              "pointsPrice": Este valor você pode calcular automaticamente, sabendo que 1 ponto corresponde a R$ 0,10,
//              "reaisCostValue": Este é o valor de custo real informado pelo usuário,
//              "isAvailable": insira sempre o valor true,
//              "categoryId": insira o id da categoria do prato informado pelo usuário, desde que exista entre as categorias pré cadastradas,
//              "dishIngredientFormDTOList": [
//                {
//                  "ingredientId": informe o id do ingrediente informado pelo usuário, desde que ela exista entre os ingredientes pré cadastrados,
//                  "quantity": informe a quantidade do ingrediente informado pelo usuário,
//                  "measurementUnitId": informe o id da unidade de medida informada pelo usuário para o ingrediente, desde que exita entra as unidades de medidas pré cadastradas
//                }
//              ]
//            }
//        """;
//
//        // Carregar dados existentes de categorias, unidades de medida e ingredientes
//        List<CategoryDTO> categoryDTOList = categoryService.list();
//        List<MeasurementUnitDTO> measurementUnitDTOList = measurementUnitService.list();
//        List<IngredientDTO> ingredientDTOList = ingredientService.list();
//
//        // Gerar a lista de dados cadastrados
//        StringBuilder registeredDataPrompt = new StringBuilder("Apenas os dados pré-cadastrados para categoria, unidade de medida e ingrediente podem ser informados pelo usuário. Os dados ppré-cadastrados são os seguintes: \n");
//
//        for (CategoryDTO categoryDTO : categoryDTOList) {
//            registeredDataPrompt.append("Categoria -> Id:  " + categoryDTO.id() + " Nome: " + categoryDTO.name() + "\n");
//        }
//        for (MeasurementUnitDTO measurementUnitDTO : measurementUnitDTOList) {
//            registeredDataPrompt.append("Unidade de Medida -> Id: " + measurementUnitDTO.id() + " Nome: " + measurementUnitDTO.name() + " Sigla: " + measurementUnitDTO.acronym() + "\n");
//        }
//        for (IngredientDTO ingredientDTO : ingredientDTOList) {
//            registeredDataPrompt.append("Ingrediente -> Id: " + ingredientDTO.id() + " Nome: " + ingredientDTO.name() + "\n");
//        }
//
//        // Enviar o prompt ao Gemini e retornar a resposta
//        //return new ChatResponseDTO(sendRequest(prompt), true);
//        return new ChatResponseDTO(dishName + "\n" + dishDescription + "\n" + dishReaisPrice + "\n" + dishReaisCostPrice
//                + dishCategoryId, false);
//    }
//
//    private String getDishNameFromUserMessage(String userMessage) throws IOException, InterruptedException {
//        String prompt = "O usuário enviou a seguinte mensagem com a intenção de criar um prato: [" + userMessage + "]." +
//                """
//                A partir dessa mensagem identifique o nome do prato e se identificado retorne apenas o nome do prato.
//                Caso não seja possível identificar o nome do prato retorne apenas a palavra [ERRO].
//
//                Exemplos:
//
//                [Quero criar um prato com nome farofa] = retorne apenas [farofa]
//                [Quero criar o prato canjica] = retorne apenas [canjica].
//                [Quero criar um prato] = retorne apenas [ERRO], pois não foi informado o nome.
//                """;
//
//        String dishName = sendRequest(prompt);
//        log.info("Nome do prato: " + dishName);
//
//        return dishName;
//    }
//
//    private String getDishDescriptionFromUserMessage(String userMessage) throws IOException, InterruptedException {
//        String prompt = "O usuário enviou a seguinte mensagem com a intenção de criar um prato: [" + userMessage + "]." +
//                """
//                A partir dessa mensagem identifique a descrição do prato e se identificado retorne apenas a descrição do prato.
//                Caso não seja possível identificar a descrição do prato retorne apenas a palavra [ERRO].
//
//                Exemplos:
//
//                [Quero criar um prato com nome farofa, com a descrição delicioso e suculento] = retorne apenas [delicioso e suculento].
//                [Quero criar um prato com nome espetinho, irresístivel sabor do restaurante carinho] = retorne apenas [irresístivel sabor do restaurante carinho].
//                [Quero criar o prato canjica, delicioso sabor de sobremesa brasileira] = retorne apenas [delicioso sabor de sobremesa brasileira].
//                [Quero criar um prato chamado costelão] = retorne apenas a palavra [ERRO], pois não foi informado uma descrição para o prato.
//                """;
//
//        String dishDescription = sendRequest(prompt);
//        log.info("Descrição do prato: " + dishDescription);
//
//        return dishDescription;
//    }
//
//    private String getDishReaisPriceFromUserMessage(String userMessage) throws IOException, InterruptedException {
//        String prompt = "O usuário enviou a seguinte mensagem com a intenção de criar um prato: [" + userMessage + "]." +
//                """
//                A partir dessa mensagem identifique o preço em reais (R$) do prato e se identificado retorne apenas o preço em reais (R$) do prato.
//                Caso não seja possível identificar o preço em reais (R$) do prato retorne apenas a palavra [ERRO].
//
//                Exemplos:
//
//                [Quero criar um prato com nome farofa, com a descrição delicioso e suculento que custe R$ 29,90] = retorne apenas [29.90].
//                [Quero criar um prato com nome espetinho, irresístivel sabor do restaurante carinho, custando 29.90 reais] = retorne apenas [29.90].
//                [Quero criar o prato canjica, delicioso sabor de sobremesa brasileira, com valor de 29 reais] = retorne apenas [29].
//                [Quero criar um prato chamado costelão, com a descrição saboroso e suculento] = retorne apenas a palavra [ERRO], pois não foi informado o preço em reais (R$) para o prato.
//                Observação: caso a pessoa tenha digitado um valor númerico incorreto para o preço, por exemplo, 30.9.9; 39,9,9; 35.0000,4, ou outros, retorne apenas [ERRO].
//                Além disso, os únicos valores númericos aceitos para o preço seguem o padrão [0-9]*.[0-9][0-9] ou [0-9]*,[0-9][0-9], sempre arredonde para 2 casas decimais.
//                """;
//
//        String dishReaisPrice = sendRequest(prompt);
//        log.info("Preço em R$ do prato: " + dishReaisPrice);
//
//        return dishReaisPrice;
//    }
//
//    private String getDishReaisCostPriceFromUserMessage(String userMessage) throws IOException, InterruptedException {
//        String prompt = "O usuário enviou a seguinte mensagem com a intenção de criar um prato: [" + userMessage + "]." +
//                """
//                A partir dessa mensagem identifique o preço de custo em reais (R$) do prato e se identificado retorne apenas o preço de custo em reais (R$) do prato.
//                Caso não seja possível identificar o preço de custo em reais (R$) do prato retorne apenas a palavra [ERRO].
//
//                Exemplos:
//
//                [Quero criar um prato com nome farofa, com a descrição delicioso e suculento que custe R$ 29,90, com preço de custo de 20 reais.] = retorne apenas [20].
//                [Quero criar um prato com nome espetinho, irresístivel sabor do restaurante carinho, custando 29.90 reais, que tem um preço real de R$ 22.00] = retorne apenas [22.00].
//                [Quero criar o prato canjica, delicioso sabor de sobremesa brasileira, com valor de 29 reais e preço de custo de R$ 15] = retorne apenas [15].
//                [Quero criar um prato chamado costelão, com a descrição saboroso e suculento, com valor de 40 reais] = retorne apenas a palavra [ERRO], pois não foi informado o preço de custo em reais (R$) para o prato.
//                Observação: caso a pessoa tenha digitado um valor númerico incorreto para o preço de custo, por exemplo, 30.9.9; 39,9,9; 35.0000,4, ou outros, retorne apenas [ERRO].
//                Além disso, os únicos valores númericos aceitos para o preço de custo seguem o padrão [0-9]*.[0-9][0-9] ou [0-9]*,[0-9][0-9], sempre arredonde para 2 casas decimais.
//                """;
//
//        String dishReaisCostPrice = sendRequest(prompt);
//        log.info("Preço de custo em R$ do prato: " + dishReaisCostPrice);
//
//        return dishReaisCostPrice;
//    }
//
//    private String getDishCategoryFromUserMessage(String userMessage) throws IOException, InterruptedException {
//        List<CategoryDTO> categoryDTOList = categoryService.list();
//
//        StringBuilder validCategoryPrompt = new StringBuilder("As categorias válidas são: \n");
//        for (CategoryDTO categoryDTO : categoryDTOList) {
//            validCategoryPrompt.append("Categoria -> Id:  " + categoryDTO.id() + " Nome: " + categoryDTO.name() + "\n");
//        }
//
//        String prompt = "O usuário enviou a seguinte mensagem com a intenção de criar um prato: [" + userMessage + "]." +
//                """
//                A partir dessa mensagem identifique categoria do prato e se identificado retorne apenas o id da categoria válido do prato.
//                Caso não seja possível identificar a categoria do prato retorne apenas a palavra [ERRO].
//                """
//                + validCategoryPrompt +
//                """
//                Exemplos:
//
//                [Quero criar um prato com nome farofa, com a descrição delicioso e suculento que custe R$ 29,90, com preço de custo de 20 reais, na categoria brasil.] = retorne apenas um numero que representa o id da categoria válida.
//                [Quero criar um prato com nome espetinho, irresístivel sabor do restaurante carinho, custando 29.90 reais, que tem um preço real de R$ 22.00, na categoria carnes] = retorne apenas um numero que representa o id da categoria válida.
//                [Quero criar o prato canjica, delicioso sabor de sobremesa brasileira, com valor de 29 reais e preço de custo de R$ 15, insira-o na categoria sobremesas] = retorne apenas um numero que representa o id da categoria válida.
//                [Quero criar um prato chamado costelão, com a descrição saboroso e suculento, com valor de 40 reais e preço de custo de 20 reais.] = retorne apenas a palavra [ERRO], pois não foi informado a categoria para o prato.
//                Observação: caso a categoria informada não esteja entre as categorias válidas, retorne [ERRO].
//                Além disso, caso o usuário informe mais de uma categoria, retorne apenas [ERRO].
//                """;
//
//        String dishCategory = sendRequest(prompt);
//        log.info(prompt);
//        log.info("Categoria do prato: " + dishCategory);
//
//        return dishCategory;
//    }

//    private ChatResponseDTO processGreetingMessage(ChatMessageDTO chatMessageDTO) throws IOException, InterruptedException {
//        String prompt = initialCommand + "O cliente enviou uma saudação: [" + chatMessageDTO.userMessage() + "]. Responda de maneira amigável e acolhedora.";
//
//        String response = sendRequest(prompt);
//
//        return new ChatResponseDTO(response, true);
//    }
//
//    private ChatResponseDTO processPresentationMessage() throws IOException, InterruptedException {
//        String prompt = initialCommand +  "O cliente quer saber mais sobre o restaurante. Explique que o restaurante tem pratos deliciosos no cardápio e que você pode ajudar o cliente a navegar pelos sabores do restaurante!";
//
//        String response = sendRequest(prompt);
//
//        return new ChatResponseDTO(response, true);
//    }
//
//    private ChatResponseDTO processOtherMessage() throws IOException, InterruptedException {
//        String prompt = initialCommand + "Retorne uma mensagem pedindo desculpas e dizendo que não conseguiu processar a mensagem do usuário.";
//
//        String response = sendRequest(prompt);
//        return new ChatResponseDTO(response, false);
//    }

}