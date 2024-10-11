package com.menumaster.restaurant.category.mapper;

import com.menumaster.restaurant.category.domain.dto.CategoryDTO;
import com.menumaster.restaurant.category.domain.model.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryToCategoryDTOMapper {

    CategoryDTO convert(Category category);
}
