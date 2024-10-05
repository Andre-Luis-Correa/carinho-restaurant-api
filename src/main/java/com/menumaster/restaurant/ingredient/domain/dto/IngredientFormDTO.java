package com.menumaster.restaurant.ingredient.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IngredientFormDTO(
        @NotBlank
        String name,

        @NotNull
        Double totalQuantityAvailable,

        @NotNull
        Long measurementUnitId
) {
}
