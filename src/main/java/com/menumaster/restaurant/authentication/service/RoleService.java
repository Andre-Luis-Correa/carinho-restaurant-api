package com.menumaster.restaurant.authentication.service;

import com.menumaster.restaurant.authentication.domain.entity.Role;
import com.menumaster.restaurant.authentication.enums.RoleName;
import com.menumaster.restaurant.authentication.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    public Optional<Role> findRoleByName(RoleName role) {
        return roleRepository.findByName(role);
    }
}
