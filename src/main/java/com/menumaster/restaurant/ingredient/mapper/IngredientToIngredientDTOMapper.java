package com.menumaster.restaurant.ingredient.mapper;

import com.menumaster.restaurant.ingredient.domain.dto.IngredientDTO;
import com.menumaster.restaurant.ingredient.domain.model.Ingredient;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IngredientToIngredientDTOMapper {

    IngredientDTO convert(Ingredient ingredient);
}
