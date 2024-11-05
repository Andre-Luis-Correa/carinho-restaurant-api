package com.menumaster.restaurant.ingredient.controller;

import com.menumaster.restaurant.ingredient.domain.dto.IngredientDTO;
import com.menumaster.restaurant.ingredient.domain.dto.IngredientFormDTO;
import com.menumaster.restaurant.ingredient.domain.model.Ingredient;
import com.menumaster.restaurant.ingredient.service.IngredientService;
import com.menumaster.restaurant.measurementunit.domain.model.MeasurementUnit;
import com.menumaster.restaurant.measurementunit.service.MeasurementUnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ingredient")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;
    private final MeasurementUnitService measurementUnitService;

    @PostMapping("/create")
    public ResponseEntity<IngredientDTO> createIngredient(@Valid @RequestBody IngredientFormDTO ingredientFormDTO) {
        MeasurementUnit measurementUnit = measurementUnitService.getOrThrowException(ingredientFormDTO.measurementUnitId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ingredientService.create(ingredientFormDTO, measurementUnit));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<IngredientDTO> updateIngredient(@PathVariable Long id, @RequestBody IngredientFormDTO ingredientFormDTO) {
        Ingredient ingredient = ingredientService.getOrThrowException(id);
        MeasurementUnit measurementUnit = measurementUnitService.getOrThrowException(ingredientFormDTO.measurementUnitId());
        return ResponseEntity.status(HttpStatus.OK).body(ingredientService.update(ingredient, measurementUnit, ingredientFormDTO));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteIngredient(@PathVariable Long id) {
        Ingredient ingredient = ingredientService.getOrThrowException(id);
        ingredientService.delete(ingredient);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/list")
    public ResponseEntity<List<IngredientDTO>> getCategoryDTOList() {
        return ResponseEntity.status(HttpStatus.OK).body(ingredientService.list());
    }

    @GetMapping("/page")
    public ResponseEntity<Page<IngredientDTO>> getCategoryDTOPage(Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(ingredientService.listByPageable(pageable));
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<IngredientDTO> getCategoryDTOById(@PathVariable Long id) {
        Ingredient ingredient = ingredientService.getOrThrowException(id);
        return ResponseEntity.status(HttpStatus.OK).body(ingredientService.convertIngredientToIngredientDTO(ingredient));
    }
}
