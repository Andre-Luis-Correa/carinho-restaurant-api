package com.menumaster.restaurant.dish.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DishFormDTO(

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
        Boolean isAvailable,

        @NotNull
        Long categoryId,

        @NotNull
        List<DishIngredientFormDTO> dishIngredientFormDTOList
) {
}
