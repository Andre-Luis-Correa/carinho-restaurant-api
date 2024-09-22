package com.menumaster.restaurant.authentication.domain.dto;

import com.menumaster.restaurant.authentication.enums.RoleName;

public record UpdateUserDTO(
        String email,

        String password,

        String name,

        RoleName role
) {
}
