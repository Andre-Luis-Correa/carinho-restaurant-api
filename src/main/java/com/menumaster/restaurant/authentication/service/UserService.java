package com.menumaster.restaurant.authentication.service;

import com.menumaster.restaurant.authentication.domain.dto.CreateUserDto;
import com.menumaster.restaurant.authentication.domain.dto.LoginUserDto;
import com.menumaster.restaurant.authentication.domain.dto.RecoveryJwtTokenDto;
import com.menumaster.restaurant.authentication.domain.entity.Role;
import com.menumaster.restaurant.authentication.domain.entity.User;
import com.menumaster.restaurant.authentication.repository.RoleRepository;
import com.menumaster.restaurant.authentication.repository.UserRepository;
import com.menumaster.restaurant.authentication.utils.UserDetailsImpl;
import com.menumaster.restaurant.security.JwtTokenService;
import com.menumaster.restaurant.security.SecurityConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class UserService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;
    private final SecurityConfiguration securityConfiguration;
    private final RoleRepository roleRepository;

    public RecoveryJwtTokenDto authenticateUser(LoginUserDto loginUserDto) {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                new UsernamePasswordAuthenticationToken(loginUserDto.email(), loginUserDto.password());

        Authentication authentication = authenticationManager.authenticate(usernamePasswordAuthenticationToken);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        return new RecoveryJwtTokenDto(jwtTokenService.generateToken(userDetails));
    }

    public User createUser(CreateUserDto createUserDto) {
        Role userRole = roleRepository.findByName(createUserDto.role())
                .orElseGet(() -> roleRepository.save(new Role(null, createUserDto.role())));

        User newUser = User.builder()
                .email(createUserDto.email())
                .password(securityConfiguration.passwordEncoder().encode(createUserDto.password()))
                .cpf(createUserDto.cpf())
                .name(createUserDto.name())
                .role(userRole)
                .build();

        return userRepository.save(newUser);
    }

}
