package com.menumaster.restaurant.ingredient.domain.dto;

import com.menumaster.restaurant.measurementunit.domain.model.MeasurementUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IngredientDTO(

        @NotNull
        Long id,

        @NotBlank
        String name,

        @NotNull
        Double totalQuantityAvailable,

        @NotNull
        MeasurementUnit measurementUnit
) { }