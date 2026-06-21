package com.banking.system.security;

import com.banking.system.model.enums.Role;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JWTAuthFilterTests {

    @Mock
    private JWTService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JWTAuthFilter jwtAuthFilter;


    @BeforeEach
    void setup() {
        jwtAuthFilter = new JWTAuthFilter(jwtService);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }


    @Test
    void doFilterInternal_shouldSetAuthentication_whenTokenValid() throws Exception {

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.isValid("valid-token")).thenReturn(true);
        when(jwtService.extractRole("valid-token")).thenReturn(Role.USER);
        when(jwtService.extractAppUserId("valid-token")).thenReturn(1L);

        jwtAuthFilter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(auth);
        assertEquals(1L, auth.getPrincipal());
        assertTrue(auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldSetAdminAuthority_whenRoleIsAdmin() throws Exception {

        when(request.getHeader("Authorization")).thenReturn("Bearer admin-token");
        when(jwtService.isValid("admin-token")).thenReturn(true);
        when(jwtService.extractRole("admin-token")).thenReturn(Role.ADMIN);
        when(jwtService.extractAppUserId("admin-token")).thenReturn(99L);

        jwtAuthFilter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(auth);
        assertTrue(auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void doFilterInternal_shouldNotSetAuthentication_whenNoAuthorizationHeader() throws Exception {

        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(filterChain, times(1)).doFilter(request, response);
        verify(jwtService, never()).isValid(any());
    }

    @Test
    void doFilterInternal_shouldNotSetAuthentication_whenHeaderDoesNotStartWithBearer() throws Exception {

        when(request.getHeader("Authorization")).thenReturn("Basic somecredentials");

        jwtAuthFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(filterChain, times(1)).doFilter(request, response);
        verify(jwtService, never()).isValid(any());
    }

    @Test
    void doFilterInternal_shouldNotSetAuthentication_whenTokenInvalid() throws Exception {

        when(request.getHeader("Authorization")).thenReturn("Bearer bad-token");
        when(jwtService.isValid("bad-token")).thenReturn(false);

        jwtAuthFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(filterChain, times(1)).doFilter(request, response);
        verify(jwtService, never()).extractRole(any());
    }

    @Test
    void doFilterInternal_shouldStillCallFilterChain_evenWhenTokenInvalid() throws Exception {

        when(request.getHeader("Authorization")).thenReturn("Bearer bad-token");
        when(jwtService.isValid("bad-token")).thenReturn(false);

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldStillCallFilterChain_whenRoleIsNull() throws Exception {

        when(request.getHeader("Authorization")).thenReturn("Bearer weird-token");
        when(jwtService.isValid("weird-token")).thenReturn(true);
        when(jwtService.extractRole("weird-token")).thenReturn(null);

        jwtAuthFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldStillCallFilterChain_whenAppUserIdIsNull() throws Exception {

        when(request.getHeader("Authorization")).thenReturn("Bearer weird-token");
        when(jwtService.isValid("weird-token")).thenReturn(true);
        when(jwtService.extractRole("weird-token")).thenReturn(Role.USER);
        when(jwtService.extractAppUserId("weird-token")).thenReturn(null);

        jwtAuthFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(filterChain, times(1)).doFilter(request, response);
    }
}