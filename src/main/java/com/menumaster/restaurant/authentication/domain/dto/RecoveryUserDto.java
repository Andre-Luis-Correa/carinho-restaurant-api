package com.menumaster.restaurant.authentication.domain.dto;

import com.menumaster.restaurant.authentication.domain.entity.Role;

import java.util.List;

public record RecoveryUserDto(

        Long id,
        String email,
        List<Role> roles

) {
}
