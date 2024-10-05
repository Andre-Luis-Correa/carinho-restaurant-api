package com.menumaster.restaurant.dish.domain.dto;

import com.menumaster.restaurant.ingredient.domain.model.Ingredient;
import jakarta.validation.constraints.NotNull;

public record DishIngredientDTO(
        @NotNull
        Long id,

        @NotNull
        Ingredient ingredient,

        @NotNull
        Double quantity
) {
}
