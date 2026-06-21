package com.banking.system.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    
    private final JWTService jwtService;

    public SecurityConfig(JWTService jwtService) {
        this.jwtService = jwtService;
    }

    @Bean
    public JWTAuthFilter jwtAuthFilter() {
        return new JWTAuthFilter(jwtService);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> 
                    response.sendError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized")
                )
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/app-users/login",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(
                jwtAuthFilter(), 
                UsernamePasswordAuthenticationFilter.class
            )
            .build();
    }
}