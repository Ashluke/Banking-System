package com.banking.system.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {
    
    public static Long getCurrentUserId() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getPrincipal() == null) {
            throw new RuntimeException("No authenticated user found");
        }

        return (Long) auth.getPrincipal();
    }

    public static boolean isAdmin() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) { 
            return false;
        }

        return auth.getAuthorities()
            .stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
