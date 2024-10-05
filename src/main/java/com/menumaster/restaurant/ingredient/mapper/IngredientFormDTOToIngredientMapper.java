package com.menumaster.restaurant.ingredient.mapper;

import com.menumaster.restaurant.ingredient.domain.dto.IngredientFormDTO;
import com.menumaster.restaurant.ingredient.domain.model.Ingredient;
import com.menumaster.restaurant.measurementunit.domain.model.MeasurementUnit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IngredientFormDTOToIngredientMapper {

    Ingredient convert(IngredientFormDTO ingredientFormDTO);
}
