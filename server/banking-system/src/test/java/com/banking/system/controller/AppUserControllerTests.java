package com.banking.system.controller;

import com.banking.system.dto.request.AppUserCreateRequestDto;
import com.banking.system.dto.request.AppUserLoginRequestDto;
import com.banking.system.dto.request.AppUserUpdateRequestDto;
import com.banking.system.dto.response.AppUserResponseDto;
import com.banking.system.dto.response.AuthResponseDto;
import com.banking.system.exception.DuplicateResourceException;
import com.banking.system.exception.InvalidCredentialsException;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.exception.UnauthorizedActionException;
import com.banking.system.model.enums.Role;
import com.banking.system.security.JWTService;
import com.banking.system.security.SecurityConfig;
import com.banking.system.services.AppUserService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AppUserController.class)
@Import(SecurityConfig.class)
public class AppUserControllerTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private AppUserService appUserService;

    @MockitoBean
    private JWTService jwtService;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }


    private UsernamePasswordAuthenticationToken userAuth(Long appUserId) {
        return new UsernamePasswordAuthenticationToken(
            appUserId,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private UsernamePasswordAuthenticationToken adminAuth(Long appUserId) {
        return new UsernamePasswordAuthenticationToken(
            appUserId,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }


    // ===================== REGISTER =====================

    @Test
    void register_shouldReturn201_whenAdmin() throws Exception {

        AppUserCreateRequestDto request = new AppUserCreateRequestDto(
            "test@example.com", "password123", Role.USER
        );

        AppUserResponseDto response = new AppUserResponseDto(
            1L, "test@example.com", Role.USER, LocalDateTime.now()
        );

        when(appUserService.register(any(AppUserCreateRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/app-users/register")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void register_shouldReturn403_whenUserRole() throws Exception {

        AppUserCreateRequestDto request = new AppUserCreateRequestDto(
            "test@example.com", "password123", Role.USER
        );

        mockMvc.perform(post("/api/app-users/register")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());

        verify(appUserService, never()).register(any());
    }

    @Test
    void register_shouldReturn401_whenUnauthenticated() throws Exception {

        AppUserCreateRequestDto request = new AppUserCreateRequestDto(
            "test@example.com", "password123", Role.USER
        );

        mockMvc.perform(post("/api/app-users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void register_shouldReturn409_whenEmailAlreadyExists() throws Exception {

        AppUserCreateRequestDto request = new AppUserCreateRequestDto(
            "test@example.com", "password123", Role.USER
        );

        when(appUserService.register(any(AppUserCreateRequestDto.class)))
            .thenThrow(new DuplicateResourceException("Email already exists"));

        mockMvc.perform(post("/api/app-users/register")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    void register_shouldReturn400_whenEmailInvalid() throws Exception {

        AppUserCreateRequestDto request = new AppUserCreateRequestDto(
            "not-an-email", "password123", Role.USER
        );

        mockMvc.perform(post("/api/app-users/register")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(appUserService, never()).register(any());
    }

    @Test
    void register_shouldReturn400_whenPasswordTooShort() throws Exception {

        AppUserCreateRequestDto request = new AppUserCreateRequestDto(
            "test@example.com", "short", Role.USER
        );

        mockMvc.perform(post("/api/app-users/register")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(appUserService, never()).register(any());
    }


    // ===================== LOGIN =====================

    @Test
    void login_shouldReturn200_whenNoAuthRequired() throws Exception {

        AppUserLoginRequestDto request = new AppUserLoginRequestDto("test@example.com", "password123");

        when(appUserService.login(any(AppUserLoginRequestDto.class)))
            .thenReturn(new AuthResponseDto("fake-jwt-token"));

        mockMvc.perform(post("/api/app-users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("fake-jwt-token"));
    }

    @Test
    void login_shouldReturn401_whenInvalidCredentials() throws Exception {

        AppUserLoginRequestDto request = new AppUserLoginRequestDto("test@example.com", "wrongPassword");

        when(appUserService.login(any(AppUserLoginRequestDto.class)))
            .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/app-users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void login_shouldReturn404_whenUserNotFound() throws Exception {

        AppUserLoginRequestDto request = new AppUserLoginRequestDto("missing@example.com", "password123");

        when(appUserService.login(any(AppUserLoginRequestDto.class)))
            .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(post("/api/app-users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void login_shouldReturn400_whenEmailBlank() throws Exception {

        AppUserLoginRequestDto request = new AppUserLoginRequestDto("", "password123");

        mockMvc.perform(post("/api/app-users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(appUserService, never()).login(any());
    }


    // ===================== GET BY ID =====================

    @Test
    void getById_shouldReturn200_whenAdmin() throws Exception {

        AppUserResponseDto response = new AppUserResponseDto(
            1L, "test@example.com", Role.USER, LocalDateTime.now()
        );

        when(appUserService.getById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/app-users/1")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void getById_shouldReturn403_whenUserRole() throws Exception {

        mockMvc.perform(get("/api/app-users/1")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isForbidden());

        verify(appUserService, never()).getById(any());
    }

    @Test
    void getById_shouldReturn404_whenNotFound() throws Exception {

        when(appUserService.getById(1L))
            .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/app-users/1")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isNotFound());
    }

    @Test
    void getById_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(get("/api/app-users/1"))
            .andExpect(status().isUnauthorized());
    }


    // ===================== GET ALL =====================

    @Test
    void getAll_shouldReturn200_whenAdmin() throws Exception {

        AppUserResponseDto response = new AppUserResponseDto(
            1L, "test@example.com", Role.USER, LocalDateTime.now()
        );

        Page<AppUserResponseDto> page = new PageImpl<>(List.of(response));

        when(appUserService.getAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/app-users")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].email").value("test@example.com"));
    }

    @Test
    void getAll_shouldReturn403_whenUserRole() throws Exception {

        mockMvc.perform(get("/api/app-users")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isForbidden());

        verify(appUserService, never()).getAll(any());
    }


    // ===================== UPDATE =====================

    @Test
    void update_shouldReturn200_whenOwner() throws Exception {

        AppUserUpdateRequestDto request = new AppUserUpdateRequestDto("new@example.com", "newPassword123");

        AppUserResponseDto response = new AppUserResponseDto(
            1L, "new@example.com", Role.USER, LocalDateTime.now()
        );

        when(appUserService.update(eq(1L), any(AppUserUpdateRequestDto.class), eq(1L), eq(false)))
            .thenReturn(response);

        mockMvc.perform(put("/api/app-users/1")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("new@example.com"));
    }

    @Test
    void update_shouldReturn200_whenAdmin() throws Exception {

        AppUserUpdateRequestDto request = new AppUserUpdateRequestDto("new@example.com", "newPassword123");

        AppUserResponseDto response = new AppUserResponseDto(
            1L, "new@example.com", Role.USER, LocalDateTime.now()
        );

        when(appUserService.update(eq(1L), any(AppUserUpdateRequestDto.class), eq(99L), eq(true)))
            .thenReturn(response);

        mockMvc.perform(put("/api/app-users/1")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
    }

    @Test
    void update_shouldReturn403_whenNotOwnerAndNotAdmin() throws Exception {

        AppUserUpdateRequestDto request = new AppUserUpdateRequestDto("new@example.com", "newPassword123");

        when(appUserService.update(eq(1L), any(AppUserUpdateRequestDto.class), eq(999L), eq(false)))
            .thenThrow(new UnauthorizedActionException("You do not own this account"));

        mockMvc.perform(put("/api/app-users/1")
                .with(authentication(userAuth(999L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    void update_shouldReturn401_whenUnauthenticated() throws Exception {

        AppUserUpdateRequestDto request = new AppUserUpdateRequestDto("new@example.com", "newPassword123");

        mockMvc.perform(put("/api/app-users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void update_shouldReturn400_whenPhoneNumberInvalid() throws Exception {

        AppUserUpdateRequestDto request = new AppUserUpdateRequestDto("", "");

        mockMvc.perform(put("/api/app-users/1")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(appUserService, never()).update(any(), any(), any(), anyBoolean());
    }


    // ===================== DELETE =====================

    @Test
    void delete_shouldReturn204_whenAdmin() throws Exception {

        doNothing().when(appUserService).deleteById(1L);

        mockMvc.perform(delete("/api/app-users/1")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isNoContent());

        verify(appUserService, times(1)).deleteById(1L);
    }

    @Test
    void delete_shouldReturn403_whenUserRole() throws Exception {

        mockMvc.perform(delete("/api/app-users/1")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isForbidden());

        verify(appUserService, never()).deleteById(any());
    }

    @Test
    void delete_shouldReturn404_whenNotFound() throws Exception {

        doThrow(new ResourceNotFoundException("User not found"))
            .when(appUserService).deleteById(1L);

        mockMvc.perform(delete("/api/app-users/1")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(delete("/api/app-users/1"))
            .andExpect(status().isUnauthorized());
    }
}