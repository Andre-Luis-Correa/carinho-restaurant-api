package com.menumaster.restaurant.dish.mapper;

import com.menumaster.restaurant.dish.domain.dto.DishDTO;
import com.menumaster.restaurant.dish.domain.dto.DishIngredientDTO;
import com.menumaster.restaurant.dish.domain.model.Dish;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DishToDishDTOMapper {

    @Mapping(source = "dishIngredientDTOList", target = "dishIngredientDTOList")
    DishDTO convert(Dish dish, List<DishIngredientDTO> dishIngredientDTOList);
}
