package com.menumaster.restaurant.category.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoryDTO(

        @NotNull
        Long id,

        @NotBlank
        String name

) { }
