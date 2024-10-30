package com.menumaster.restaurant.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.menumaster.restaurant.aiassistant.domain.dto.ChatMessageDTO;
import com.menumaster.restaurant.aiassistant.domain.dto.ChatResponseDTO;
import com.menumaster.restaurant.category.domain.dto.CategoryDTO;
import com.menumaster.restaurant.category.domain.model.Category;
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
        intents.add("5. Se o cliente deseja visualizar as informações de um prato específico, retorne apenas a palavra entre colchetes [VIEW_SPECIFIC_DISH_MENU].");
        intents.add("6. Se o cliente estiver interessado em visualizar os pratos do cardápio em uma categoria específica, retorne apenas a palavra entre colchetes [VIEW_SPECIFIC_DISH_CATEGORY].");
        intents.add("7. Se o cliente estiver interessado em visualizar os pratos do cardápio que possuem um determinado ingrediente, retorne apenas a palavra entre colchetes [VIEW_SPECIFIC_DISH_INGREDIENT].");
        intents.add("8. Se o cliente estiver interessado em visualizar o cardápio completo, retorne apenas a palavra entre colchetes [VIEW_MENU].");
        intents.add("9. Se a mensagem do cliente não se encaixou em nenhuma das regras anteriores, então retorne apenas uma mensagem pedindo desculpas e explicando que não conseguiu interpretar a mensagem do cliente.");

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
        if(intent.contains("VIEW_SPECIFIC_DISH_MENU")) intent = "VIEW_SPECIFIC_DISH_MENU";
        if(intent.contains("VIEW_SPECIFIC_DISH_CATEGORY")) intent = "VIEW_SPECIFIC_DISH_CATEGORY";
        if(intent.contains("VIEW_SPECIFIC_DISH_INGREDIENT")) intent = "VIEW_SPECIFIC_DISH_INGREDIENT";
        if(intent.contains("VIEW_MENU")) intent = "VIEW_MENU";
        log.info("Intenção tratada:" + intent);
        return switch (intent) {
            case "CREATE_DISH" -> processCreateDishMessage(chatMessageDTO);
            case "DELETE_DISH" -> processDeleteDishMessage(chatMessageDTO);
            case "VIEW_SPECIFIC_DISH_MENU" -> processViewSpecificDishMessage(chatMessageDTO);
            case "VIEW_SPECIFIC_DISH_CATEGORY" -> processViewSpecificDishCategoryMessage(chatMessageDTO);
            case "VIEW_SPECIFIC_DISH_INGREDIENT" -> processViewSpecificDishIngredientMessage(chatMessageDTO);
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

        Category category;

        if(categoryService.existById(dishFormDTO.categoryId())) {
            category = categoryService.getOrThrowException(dishFormDTO.categoryId());
        } else {
            return new ChatResponseDTO(sendRequest("Retorne uma breve mensagem de desculpa explicando que a categoria informada pelo cliente é inválida."), false);
        }

        List<DishIngredient> dishIngredientList = new ArrayList<>();
        for(DishIngredientFormDTO dishIngredientFormDTO : dishFormDTO.dishIngredientFormDTOList()) {
            Ingredient ingredient;
            MeasurementUnit measurementUnit;

            if(ingredientService.existsById(dishIngredientFormDTO.ingredientId())){
                ingredient = ingredientService.getOrThrowException(dishIngredientFormDTO.ingredientId());
            } else {
                return new ChatResponseDTO(sendRequest("Retorne uma breve mensagem de desculpa explicando que os ingredientes informados pelo cliente são inválidos."), false);
            }

            if(measurementUnitService.existsById(dishIngredientFormDTO.measurementUnitId())){
                measurementUnit = measurementUnitService.getOrThrowException(dishIngredientFormDTO.measurementUnitId());
            } else {
                return new ChatResponseDTO(sendRequest("Retorne uma breve mensagem de desculpa explicando que as unidades de medidas informadas pelo cliente são inválidas."), false);
            }

            DishIngredient dishIngredient = dishService.convertToDishIngredient(null, ingredient, measurementUnit, dishIngredientFormDTO);
            dishIngredientList.add(dishIngredient);
        }

        DishDTO dishDTO = dishService.create(category, dishIngredientList, dishFormDTO);

        String promptToIntroduceCreatedDish = String.format("Retorne uma mensagem de prato criado com sucesso. Apresente ao cliente o novo prato criado de forma criativa. As informações do novo prato são: nome = %s, descrição = %s. ", dishDTO.name(), dishDTO.description());

        return new ChatResponseDTO(sendRequest(promptToIntroduceCreatedDish), true);
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
            promptIntroduceSpecificDishes.append("No entanto, não foi possível encontrar nenhum prato solicitado pelo cliente, retorne uma mensagem simples e breve explicando que não foi possível encontrar as informações dos pratos solicitados.");
        } else {
            promptIntroduceSpecificDishes.append("Sendo assim, apresente ao cliente de maneira amigável e divertida as informações dos seguintes pratos: \n");
            int i = 1;
            for(Dish dish : foundDishes) {
                promptIntroduceSpecificDishes.append(i++).append("Prato: ").append(dish.getName()).append(", Descrição: ").append(dish.getDescription()).append(", Preço (R$): ").append(dish.getReaisPrice()).append(", ");
                List<DishIngredient> dishIngredientList = dishService.listAllByDish(dish);
                promptIntroduceSpecificDishes.append("Ingredientes: ");
                int j = 1;
                for(DishIngredient dishIngredient : dishIngredientList) {
                    promptIntroduceSpecificDishes.append(j++).append(" - ").append(dishIngredient.getIngredient().getName());
                }
                promptIntroduceSpecificDishes.append("\n");
            }
        }

        if(!notFoundDishes.isEmpty()) {
            promptIntroduceSpecificDishes.append("Além disso, retorne uma mensagem simples e breve explicando que não conseguiu encontrar informações a respeito dos seguintes pratos: ");
            int i = 1;
            for(String dish : notFoundDishes) {
                promptIntroduceSpecificDishes.append(i++).append("Prato: ").append(dish).append("\n");
            }
        }

        return new ChatResponseDTO(sendRequest(promptIntroduceSpecificDishes.toString()), true);
    }

    private ChatResponseDTO processViewSpecificDishCategoryMessage(ChatMessageDTO chatMessageDTO) throws IOException, InterruptedException {
        List<CategoryDTO> allCategoryList = categoryService.list();
        log.info("TESTE AQUI");
        StringBuilder validCategoriesPrompt = new StringBuilder();
        if (!allCategoryList.isEmpty()) {
            validCategoriesPrompt.append("\nAs categorias válidas são:\n");
            for (CategoryDTO category : allCategoryList) {
                validCategoriesPrompt.append("Categoria válida -> Id: ").append(category.id()).append(", com nome: ").append(category.name()).append("\n");
            }
        } else {
            validCategoriesPrompt.append("\nNão existe nenhuma categoria válida cadastrada\n");
            String sorryPrompt = "Retorne uma mensagem simples e breve explicando que a categoria informada não é válida.";
            return new ChatResponseDTO(sendRequest(sorryPrompt), false);
        }
        log.info("TESTE AQUI");
        String promptGetCategoriesNames = validCategoriesPrompt +  "A partir da seguinte frase enviada pelo usuário: ["
                + chatMessageDTO.userMessage() +
                """
                ], retorne apenas e somente, sem nenhuma mensagem adicional, extraia somente os nomes das categorias que o usuário digitou na mensagem que ele enviou.
                Caso o usuário informe  apenas categorias que não existem, então retorne apenas a palavra
                entre colchetes [ERRO]. Caso todas as categorias digitadas pelo usuário sejam válidas, você deve retornar uma mensagem onde
                cada nome válido de categoria deve ser separado por um ponto e vírgula [;], caso haja apenas o nome de uma categoria, 
                retorne apenas o nome dele sem [;]
                """;

        log.info(promptGetCategoriesNames);
        String categoriesName = sendRequest(promptGetCategoriesNames);
        log.info(categoriesName);
        if(categoriesName.contains("ERRO")) {
            String sorryPrompt = "Retorne uma mensagem simples e breve explicando que a categoria informada não é válida.";
            return new ChatResponseDTO(sendRequest(sorryPrompt), false);
        }

        String[] categoryNameList = categoriesName.split(";");

        List<Category> foundCategories = new ArrayList<>();
        List<String> notFoundCategories = new ArrayList<>();

        for(String categoryName: categoryNameList) {
            String trimmedCategoryName = categoryName.trim();
            List<Category> categoryList = categoryService.findByNameContainingIgnoreCase(trimmedCategoryName);


            if (!categoryList.isEmpty()) foundCategories.addAll(categoryList);
            else notFoundCategories.add(categoryName);
        }

        StringBuilder promptIntroduceSpecificDishesWithCategory = new StringBuilder("Não envie nenhuma saudação ao cliente. O cliente solicitou para visualizar informações de alguns pratos específicos do cardápio de acordo com categoria. ");

        if(foundCategories.isEmpty()) {
            promptIntroduceSpecificDishesWithCategory.append("No entanto, não foi possível encontrar nenhuma categoria solicitada pelo cliente, peça desculpas e diga que não foi possível encontrar a categoria solicitada. Além disso, recomende as seguintes categorias válidas: " + validCategoriesPrompt);
        } else {
            promptIntroduceSpecificDishesWithCategory.append("Sendo assim, apresente ao cliente de maneira amigável e divertida as informações dos seguintes pratos: \n");
            int i = 1;
            for(Category category : foundCategories) {
                List<Dish> dishList = dishService.listAllByCategory(category);

                if(!dishList.isEmpty()) {
                    promptIntroduceSpecificDishesWithCategory.append("Pratos na categoria ").append(category.getName()).append(": \n");

                    for (Dish dish : dishList) {
                        promptIntroduceSpecificDishesWithCategory.append(i++).append("Prato: ").append(dish.getName()).append(", Descrição: ").append(dish.getDescription()).append(", Preço (R$): ").append(dish.getReaisPrice()).append(", Category: ").append(category.getName()).append("\n");
                    }

                    i = 1;
                } else {
                    promptIntroduceSpecificDishesWithCategory.append("\nNão foi possível encontrar pratos na categoria ").append(category.getName());
                }

            }
        }

        if(!notFoundCategories.isEmpty()) {
            promptIntroduceSpecificDishesWithCategory.append("Além disso, retorne uma mensagem simples e breve explicando que não conseguiu encontrar informações a respeito dos seguintes categorias: ");
            int i = 1;
            for(String categoryName : notFoundCategories) {
                promptIntroduceSpecificDishesWithCategory.append(i++).append("Category: ").append(categoryName).append("\n");
            }
        }

        return new ChatResponseDTO(sendRequest(promptIntroduceSpecificDishesWithCategory.toString()), true);
    }

    private ChatResponseDTO processViewSpecificDishIngredientMessage(ChatMessageDTO chatMessageDTO) throws IOException, InterruptedException {
        List<IngredientDTO> allIngredientList = ingredientService.list();

        StringBuilder validIngredientsPrompt = new StringBuilder();
        if (!allIngredientList.isEmpty()) {
            validIngredientsPrompt.append("\nOs ingredientes válidos são:\n");
            for (IngredientDTO ingredient : allIngredientList) {
                validIngredientsPrompt.append("Ingrediente válido -> Id: ").append(ingredient.id()).append(", com nome: ").append(ingredient.name()).append("\n");
            }
        } else {
            validIngredientsPrompt.append("\nNão existe nenhum ingrediente válido cadastrado\n");
        }

        String promptGetIngredientNames = validIngredientsPrompt + "A partir da seguinte frase enviada pelo usuário: ["
                + chatMessageDTO.userMessage() +
                """
                ], retorne apenas e somente, sem nenhuma mensagem adicional, extraia somente os nomes dos ingredientes que o usuário digitou na mensagem que ele enviou.
                Caso o usuário informe apenas ingredientes que não existem, então retorne apenas a palavra
                entre colchetes [ERRO]. Caso todos os ingredientes digitados pelo usuário sejam válidos, você deve retornar uma mensagem onde
                cada nome válido de ingrediente deve ser separado por um ponto e vírgula [;], caso haja apenas o nome de um ingrediente, 
                retorne apenas o nome dele sem [;]
                """;

        log.info(promptGetIngredientNames);

        String ingredientsNames = sendRequest(promptGetIngredientNames);
        log.info(ingredientsNames);
        if(ingredientsNames.contains("ERRO")) {
            String sorryPrompt = "Retorne uma mensagem simples e breve explicando apenas que os ingredientes informados são inválidos.";
            return new ChatResponseDTO(sendRequest(sorryPrompt), false);
        }

        String[] ingredientNameList = ingredientsNames.split(";");

        List<Ingredient> foundIngredients = new ArrayList<>();
        List<String> notFoundIngredients = new ArrayList<>();

        for(String ingredientName : ingredientNameList) {
            String trimmedIngredientName = ingredientName.trim();
            List<Ingredient> ingredientList = ingredientService.findByNameContainingIgnoreCase(trimmedIngredientName);

            if (!ingredientList.isEmpty()){
                log.info("achei o ingrediente " + ingredientName);
                foundIngredients.addAll(ingredientList);
            }
            else notFoundIngredients.add(ingredientName);
        }

        StringBuilder promptIntroduceSpecificDishesWithIngredient = new StringBuilder("Não envie nenhuma saudação ao cliente. O cliente solicitou para visualizar informações de alguns pratos específicos do cardápio de acordo com ingredientes. ");

        if(foundIngredients.isEmpty()) {
            promptIntroduceSpecificDishesWithIngredient.append("No entanto, não foi possível encontrar nenhum ingrediente solicitado pelo cliente, retorne uma mensagem simples e breve explicando que não foi possível encontrar pratos com o ingrediente solicitado.");
        } else {
            promptIntroduceSpecificDishesWithIngredient.append("Sendo assim, apresente ao cliente de maneira amigável e divertida as informações dos seguintes pratos: \n");
            int i = 1;
            for(Ingredient ingredient : foundIngredients) {
                log.info(ingredient.getName());
                List<DishIngredient> dishIngredientList = dishService.listAllByIngredient(ingredient);

                if(!dishIngredientList.isEmpty()) {
                    promptIntroduceSpecificDishesWithIngredient.append("Pratos com o ingrediente ").append(ingredient.getName()).append(": \n");

                    for (DishIngredient dishIngredient : dishIngredientList) {
                        promptIntroduceSpecificDishesWithIngredient.append(i++).append("Prato: ").append(dishIngredient.getDish().getName()).append(", Descrição: ").append(dishIngredient.getDish().getDescription()).append(", Preço (R$): ").append(dishIngredient.getDish().getReaisPrice()).append(", Ingrediente: ").append(ingredient.getName()).append("\n");
                    }

                    i = 1;
                } else {
                    promptIntroduceSpecificDishesWithIngredient.append("\nNão foi possível encontrar pratos que tenham o ingrediente ").append(ingredient.getName());
                }
            }
        }

        if(!notFoundIngredients.isEmpty()) {
            promptIntroduceSpecificDishesWithIngredient.append("Além disso, retorne uma mensagem simples e breve explicando que não conseguiu encontrar informações a respeito de pratos com os seguintes ingredientes: ");
            int i = 1;
            for(String ingredientName : notFoundIngredients) {
                promptIntroduceSpecificDishesWithIngredient.append(i++).append("Ingrediente: ").append(ingredientName).append("\n");
            }
        }

        return new ChatResponseDTO(sendRequest(promptIntroduceSpecificDishesWithIngredient.toString()), true);
    }


    private ChatResponseDTO processViewMenuMessage() throws IOException, InterruptedException {
        List<DishDTO> dishDTOList = dishService.list();
        StringBuilder prompt = new StringBuilder("Não cumprimente o cliente. O cliente solicitou para visualizar os pratos disponíveis no cardápio. ");

        if(dishDTOList.isEmpty()) {
            prompt.append("No entanto, o cardápio está vazio. Retorne uma mensagem simples e breve explicando que não há pratos no cardápio.");
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

}