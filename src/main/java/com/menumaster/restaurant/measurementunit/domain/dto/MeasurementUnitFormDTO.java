package com.menumaster.restaurant.measurementunit.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MeasurementUnitFormDTO(

        @NotBlank
        String name,

        @NotBlank
        String acronym
) {
}
