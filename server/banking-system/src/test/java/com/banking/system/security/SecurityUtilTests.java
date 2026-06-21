package com.banking.system.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityUtilTests {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }


    // ===================== getCurrentUserId =====================

    @Test
    void getCurrentUserId_shouldReturnId_whenAuthenticated() {

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            42L,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        Long result = SecurityUtil.getCurrentUserId();

        assertEquals(42L, result);
    }

    @Test
    void getCurrentUserId_shouldThrowException_whenNoAuthentication() {

        SecurityContextHolder.clearContext();

        assertThrows(RuntimeException.class, () ->
            SecurityUtil.getCurrentUserId()
        );
    }

    @Test
    void getCurrentUserId_shouldThrowException_whenPrincipalIsNull() {

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            null,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(RuntimeException.class, () ->
            SecurityUtil.getCurrentUserId()
        );
    }

    @Test
    void getCurrentUserId_shouldThrowClassCastException_whenPrincipalNotLong() {

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            "not-a-long",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(ClassCastException.class, () ->
            SecurityUtil.getCurrentUserId()
        );
    }


    // ===================== isAdmin =====================

    @Test
    void isAdmin_shouldReturnTrue_whenUserHasAdminRole() {

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            1L,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        assertTrue(SecurityUtil.isAdmin());
    }

    @Test
    void isAdmin_shouldReturnFalse_whenUserHasOnlyUserRole() {

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            1L,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        assertFalse(SecurityUtil.isAdmin());
    }

    @Test
    void isAdmin_shouldReturnTrue_whenUserHasMultipleRolesIncludingAdmin() {

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            1L,
            null,
            List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
            )
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        assertTrue(SecurityUtil.isAdmin());
    }

    @Test
    void isAdmin_shouldReturnFalse_whenNoAuthentication() {

        SecurityContextHolder.clearContext();

        assertFalse(SecurityUtil.isAdmin());
    }
}