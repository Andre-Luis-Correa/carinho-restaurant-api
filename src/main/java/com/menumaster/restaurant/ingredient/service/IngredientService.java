package com.menumaster.restaurant.ingredient.service;

import com.menumaster.restaurant.exception.type.EntityNotFoundException;
import com.menumaster.restaurant.ingredient.domain.dto.IngredientDTO;
import com.menumaster.restaurant.ingredient.domain.dto.IngredientFormDTO;
import com.menumaster.restaurant.ingredient.domain.model.Ingredient;
import com.menumaster.restaurant.ingredient.mapper.IngredientFormDTOToIngredientMapper;
import com.menumaster.restaurant.ingredient.mapper.IngredientToIngredientDTOMapper;
import com.menumaster.restaurant.ingredient.repository.IngredientRepository;
import com.menumaster.restaurant.measurementunit.domain.model.MeasurementUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final IngredientFormDTOToIngredientMapper ingredientFormDTOToIngredientMapper;
    private final IngredientToIngredientDTOMapper ingredientToIngredientDTOMapper;

    public IngredientDTO create(IngredientFormDTO ingredientFormDTO, MeasurementUnit measurementUnit) {
        Ingredient ingredientToBeSaved = convertIngredientFormDTOToIngredient(ingredientFormDTO);
        ingredientToBeSaved.setMeasurementUnit(measurementUnit);

        Ingredient ingredientSaved = ingredientRepository.save(ingredientToBeSaved);
        return convertIngredientToIngredientDTO(ingredientSaved);
    }

    public IngredientDTO convertIngredientToIngredientDTO(Ingredient ingredient) {
        return ingredientToIngredientDTOMapper.convert(ingredient);
    }

    public Ingredient convertIngredientFormDTOToIngredient(IngredientFormDTO ingredientFormDTO) {
        return ingredientFormDTOToIngredientMapper.convert(ingredientFormDTO);
    }

    public Ingredient getOrThrowException(Long id) {
        return ingredientRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Ingredient", id.toString()));
    }

    public IngredientDTO update(Ingredient ingredient, MeasurementUnit measurementUnit, IngredientFormDTO ingredientFormDTO) {
        if(ingredientFormDTO.name() != null && !ingredientFormDTO.name().isBlank()) {
            ingredient.setName(ingredientFormDTO.name());
        }
        if(ingredientFormDTO.totalQuantityAvailable() != null) {
            ingredient.setTotalQuantityAvailable(ingredientFormDTO.totalQuantityAvailable());
        }
        if(measurementUnit != null) {
            ingredient.setMeasurementUnit(measurementUnit);
        }

        Ingredient ingredientUpdated = ingredientRepository.save(ingredient);
        return convertIngredientToIngredientDTO(ingredientUpdated);
    }

    public void delete(Ingredient ingredient) {
        ingredientRepository.delete(ingredient);
    }

    public List<IngredientDTO> list() {
        List<Ingredient> ingredientList = ingredientRepository.findAll();
        return ingredientList.stream().map(this::convertIngredientToIngredientDTO).toList();
    }

    public Page<IngredientDTO> listByPageable(Pageable pageable) {
        Page<Ingredient> ingredientPage = ingredientRepository.findAll(pageable);
        return ingredientPage.map(this::convertIngredientToIngredientDTO);
    }

    public Ingredient getOrNull(Long id) {
        if(ingredientRepository.findById(id).isEmpty()) {
            return null;
        }
        return ingredientRepository.findById(id).get();
    }

    public boolean existsById(Long id) {
        return ingredientRepository.existsById(id);
    }
}
