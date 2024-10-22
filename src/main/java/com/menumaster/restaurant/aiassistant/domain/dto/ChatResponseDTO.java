package com.menumaster.restaurant.aiassistant.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChatResponseDTO(
        @NotBlank
        String response,

        @NotNull
        Boolean isValidResponse
) {
}
