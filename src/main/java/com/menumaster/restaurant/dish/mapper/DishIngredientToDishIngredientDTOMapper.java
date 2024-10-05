package com.menumaster.restaurant.dish.mapper;

import com.menumaster.restaurant.dish.domain.dto.DishIngredientDTO;
import com.menumaster.restaurant.dish.domain.model.DishIngredient;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DishIngredientToDishIngredientDTOMapper {

    DishIngredientDTO convert(DishIngredient dishIngredient);
}
