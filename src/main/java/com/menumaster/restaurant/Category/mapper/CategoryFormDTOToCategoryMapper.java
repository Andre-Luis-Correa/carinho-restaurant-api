package com.menumaster.restaurant.category.mapper;

import com.menumaster.restaurant.category.domain.dto.CategoryFormDTO;
import com.menumaster.restaurant.category.domain.model.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryFormDTOToCategoryMapper {

    Category convert(CategoryFormDTO categoryFormDTO);
}
