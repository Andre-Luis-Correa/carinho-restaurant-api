package com.menumaster.restaurant.dish.domain.dto;

import com.menumaster.restaurant.category.domain.model.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DishDTO(

        @NotNull
        Long id,

        @NotBlank
        String name,

        @NotBlank
        String description,

        @NotNull
        Double reaisPrice,

        @NotNull
        Integer pointsPrice,

        @NotNull
        Double reaisCostValue,

        @NotBlank
        String urlImage,

        @NotNull
        boolean isAvailable,

        @NotNull
        Category category,

        @NotNull
        List<DishIngredientDTO> dishIngredientDTOList
) {
}
