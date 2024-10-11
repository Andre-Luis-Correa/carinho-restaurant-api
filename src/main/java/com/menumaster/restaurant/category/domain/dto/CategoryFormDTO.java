package com.menumaster.restaurant.category.domain.dto;


import jakarta.validation.constraints.NotBlank;

public record CategoryFormDTO(

        @NotBlank
        String name

) { }