package com.menumaster.restaurant.dish.controller;

import com.menumaster.restaurant.category.domain.model.Category;
import com.menumaster.restaurant.category.service.CategoryService;
import com.menumaster.restaurant.dish.domain.dto.DishDTO;
import com.menumaster.restaurant.dish.domain.dto.DishFormDTO;
import com.menumaster.restaurant.dish.domain.dto.DishIngredientFormDTO;
import com.menumaster.restaurant.dish.domain.model.Dish;
import com.menumaster.restaurant.dish.domain.model.DishIngredient;
import com.menumaster.restaurant.dish.service.DishService;
import com.menumaster.restaurant.gemini.GeminiService;
import com.menumaster.restaurant.ingredient.domain.model.Ingredient;
import com.menumaster.restaurant.ingredient.service.IngredientService;
import com.menumaster.restaurant.transcription.TranscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@RestController
@RequestMapping("/dish")
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;
    private final CategoryService categoryService;
    private final IngredientService ingredientService;
    private final GeminiService geminiService;
    private final TranscriptionService transcriptionService;

    @PostMapping("/create")
    public ResponseEntity<DishDTO> create(@Valid @RequestBody DishFormDTO dishFormDTO) throws IOException {
        Category category = categoryService.getOrThrowException(dishFormDTO.categoryId());

        dishService.verifyNoDuplicatedIngredients(dishFormDTO.dishIngredientFormDTOList());

        List<DishIngredient> dishIngredientList = new ArrayList<>();
        for(DishIngredientFormDTO dishIngredientFormDTO : dishFormDTO.dishIngredientFormDTOList()) {
            Ingredient ingredient = ingredientService.getOrThrowException(dishIngredientFormDTO.ingredientId());
            DishIngredient dishIngredient = dishService.convertToDishIngredient(null, ingredient, dishIngredientFormDTO);
            dishIngredientList.add(dishIngredient);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(dishService.create(category, dishIngredientList, dishFormDTO));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<DishDTO> update(@PathVariable Long id, @RequestBody DishFormDTO dishFormDTO) throws IOException {
        Dish dish = dishService.getOrThrowException(id);
        Category category = categoryService.getOrThrowException(dishFormDTO.categoryId());

        dishService.verifyNoDuplicatedIngredients(dishFormDTO.dishIngredientFormDTOList());

        List<DishIngredient> dishIngredientList = new ArrayList<>();
        for(DishIngredientFormDTO dishIngredientFormDTO : dishFormDTO.dishIngredientFormDTOList()) {
            Ingredient ingredient = ingredientService.getOrThrowException(dishIngredientFormDTO.ingredientId());
            DishIngredient dishIngredient = dishService.convertToDishIngredient(dish, ingredient, dishIngredientFormDTO);
            dishIngredientList.add(dishIngredient);
        }

        return ResponseEntity.status(HttpStatus.OK).body(dishService.update(dish, category, dishIngredientList, dishFormDTO));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Dish dish = dishService.getOrThrowException(id);
        dishService.delete(dish);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/list")
    public ResponseEntity<List<DishDTO>> getDishDTOList() {
        return ResponseEntity.status(HttpStatus.OK).body(dishService.list());
    }

    @GetMapping("/page")
    public ResponseEntity<Page<DishDTO>> getDishDTOPage(Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(dishService.listByPageable(pageable));
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<DishDTO> getDishDTOById(@PathVariable Long id) {
        Dish dish = dishService.getOrThrowException(id);
        return ResponseEntity.status(HttpStatus.OK).body(dishService.convertDishToDishDTO(dish));
    }

    @PostMapping("/chat")
    public ResponseEntity<String> talkToGemini(String prompt) throws IOException, InterruptedException {
        return ResponseEntity.status(HttpStatus.OK).body(geminiService.sendRequest(prompt));
    }


    @PostMapping("/transcribe-gemini")
    public String transcribeAudioWithGemini(@RequestParam("file") MultipartFile audioFile) {
        // Primeiro, fazemos o upload do áudio e obtemos o URI
        String fileUri = transcriptionService.uploadAudio(audioFile);

        if (fileUri.startsWith("Erro")) {
            // Se ocorrer um erro durante o upload, retorne o erro
            return fileUri;
        }

        try {
            // Envia o URI para a transcrição do áudio e retorna a resposta
            String transcriptionResponse = transcriptionService.transcribeAudioWithGemini2(fileUri);
            return transcriptionResponse;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "Erro ao transcrever o áudio: " + e.getMessage();
        }
    }
}
