package com.menumaster.restaurant.dish.mapper;

import com.menumaster.restaurant.dish.domain.dto.DishIngredientFormDTO;
import com.menumaster.restaurant.dish.domain.model.DishIngredient;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DishIngredientFormDTOToDishIngredientMapper {

    DishIngredient convert(DishIngredientFormDTO dishIngredientFormDTO);
}
