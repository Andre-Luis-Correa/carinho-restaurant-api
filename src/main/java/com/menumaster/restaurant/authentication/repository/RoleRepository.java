package com.menumaster.restaurant.authentication.repository;

import com.menumaster.restaurant.authentication.domain.entity.Role;
import com.menumaster.restaurant.authentication.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName role);

}
