package com.menumaster.restaurant.aiassistant.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatMessageDTO(

        @NotBlank
        String username,

        @NotBlank
        String userRole,

        @NotBlank
        String userMessage
) {
}
