package com.menumaster.restaurant.security;

import com.menumaster.restaurant.authentication.utils.UserAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final UserAuthenticationFilter userAuthenticationFilter;

    public static final String[] ENDPOINTS_WITH_AUTHENTICATION_NOT_REQUIRED = {
            // authentication
            "/authentication/login",
            "/authentication/create-user",
            "/authentication/validate-token",

            // swagger
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",

            // category
            "/category/list",
            "/category/page",

            // measurementUnit
            "/measurement-unit/list",
            "/measurement-unit/page",

            // ingredient
            "/ingredient/list",
            "/ingredient/page",
            "/ingredient/get/{id}",

            // dish
            "/dish/list",
            "/dish/page",
            "/dish/get/{id}",

            // gemini
            "/dish/chat",
            "/dish/transcribe"
    };

    public static final String[] ENDPOINTS_WITH_AUTHENTICATION_REQUIRED = {

    };

    public static final String[] ENDPOINTS_CUSTOMER = {

    };

    public static final String[] ENDPOINTS_ADMIN = {
            // category
            "/category/create",
            "/category/update/{id}",
            "/category/delete/{id}",

            // measurementUnit
            "/measurement-unit/create",
            "/measurement-unit/update/{id}",
            "/measurement-unit/delete/{id}",

            // ingredient
            "/ingredient/create",
            "/ingredient/update/{id}",
            "/ingredient/delete/{id}",

            // dish
            "/dish/create",
            "/dish/update/{id}",
            "/dish/remove-ingredient/{id}",
            "/dish/delete/{id}"

    };

    public static final String[] ENDPOINTS_ATTENDANT = {

    };


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .cors(cors -> cors
                        .configurationSource(request -> {
                            var corsConfiguration = new org.springframework.web.cors.CorsConfiguration();
                            corsConfiguration.setAllowedOriginPatterns(List.of("*"));
                            corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                            corsConfiguration.setAllowedHeaders(List.of("*"));
                            corsConfiguration.setAllowCredentials(true);
                            return corsConfiguration;
                        })
                )
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(ENDPOINTS_WITH_AUTHENTICATION_NOT_REQUIRED).permitAll()
                        .requestMatchers(ENDPOINTS_WITH_AUTHENTICATION_REQUIRED).authenticated()
                        .requestMatchers(ENDPOINTS_ADMIN).hasRole("ADMINISTRATOR")
                        .requestMatchers(ENDPOINTS_CUSTOMER).hasRole("CUSTOMER")
                        .requestMatchers(ENDPOINTS_ATTENDANT).hasRole("ATTENDANT")
                        .anyRequest().denyAll()
                )
                .addFilterBefore(userAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}