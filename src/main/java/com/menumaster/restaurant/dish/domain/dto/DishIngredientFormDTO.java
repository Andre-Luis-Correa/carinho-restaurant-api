package com.menumaster.restaurant.dish.domain.dto;

import jakarta.validation.constraints.NotNull;

public record DishIngredientFormDTO(

        @NotNull
        Long ingredientId,

        @NotNull
        Double quantity
) { }
