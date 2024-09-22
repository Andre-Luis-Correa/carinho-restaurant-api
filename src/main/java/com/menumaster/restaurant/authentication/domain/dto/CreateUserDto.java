package com.menumaster.restaurant.authentication.domain.dto;

import com.menumaster.restaurant.authentication.enums.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserDto(
        @NotBlank
        @Email
        String email,

        @NotBlank
        String password,

        @NotBlank
        String cpf,

        @NotBlank
        String name,

        @NotNull
        RoleName role
) { }
