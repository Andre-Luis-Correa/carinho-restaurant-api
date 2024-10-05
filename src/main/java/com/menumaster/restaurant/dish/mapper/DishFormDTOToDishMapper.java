package com.menumaster.restaurant.dish.mapper;

import com.menumaster.restaurant.dish.domain.dto.DishFormDTO;
import com.menumaster.restaurant.dish.domain.model.Dish;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DishFormDTOToDishMapper {

    @Mapping(source = "dishFormDTO.isAvailable", target = "isAvailable")
    Dish convert(DishFormDTO dishFormDTO);
}
