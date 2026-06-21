package com.banking.system.security;

import com.banking.system.model.enums.Role;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JWTAuthFilter extends OncePerRequestFilter {

    private final JWTService jwtService;

    public JWTAuthFilter(JWTService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, 
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {

            String token = authHeader.substring(7);

            if (jwtService.isValid(token)) {

                Role role = jwtService.extractRole(token);
                Long appUserId = jwtService.extractAppUserId(token);

                if (role != null && appUserId != null) {

                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));

                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        appUserId,
                        null,
                        authorities
                    );

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}