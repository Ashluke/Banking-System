package com.banking.system.controller;

import com.banking.system.dto.request.UserCreateRequestDto;
import com.banking.system.dto.request.UserUpdateRequestDto;
import com.banking.system.dto.response.UserResponseDto;
import com.banking.system.exception.DuplicateResourceException;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.exception.UnauthorizedActionException;
import com.banking.system.security.JWTService;
import com.banking.system.security.SecurityConfig;
import com.banking.system.services.UserService;

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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
public class UserControllerTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private UserService userService;

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


    // ===================== CREATE =====================

    @Test
    void create_shouldReturn201_whenAdmin() throws Exception {

        UserCreateRequestDto request = new UserCreateRequestDto(
            1L, "John", "Doe", "09171234567", "123 Main St"
        );

        UserResponseDto response = new UserResponseDto(
            1L, "John", "Doe", "09171234567", "123 Main St", 1L
        );

        when(userService.createUser(any(UserCreateRequestDto.class), eq(99L)))
            .thenReturn(response);

        mockMvc.perform(post("/api/users")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void create_shouldReturn403_whenUserRole() throws Exception {

        UserCreateRequestDto request = new UserCreateRequestDto(
            1L, "John", "Doe", "09171234567", "123 Main St"
        );

        mockMvc.perform(post("/api/users")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());

        verify(userService, never()).createUser(any(), any());
    }

    @Test
    void create_shouldReturn401_whenUnauthenticated() throws Exception {

        UserCreateRequestDto request = new UserCreateRequestDto(
            1L, "John", "Doe", "09171234567", "123 Main St"
        );

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void create_shouldReturn404_whenAppUserNotFound() throws Exception {

        UserCreateRequestDto request = new UserCreateRequestDto(
            1L, "John", "Doe", "09171234567", "123 Main St"
        );

        when(userService.createUser(any(UserCreateRequestDto.class), eq(99L)))
            .thenThrow(new ResourceNotFoundException("AppUser not found"));

        mockMvc.perform(post("/api/users")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void create_shouldReturn409_whenProfileAlreadyExists() throws Exception {

        UserCreateRequestDto request = new UserCreateRequestDto(
            1L, "John", "Doe", "09171234567", "123 Main St"
        );

        when(userService.createUser(any(UserCreateRequestDto.class), eq(99L)))
            .thenThrow(new DuplicateResourceException("User profile already exists"));

        mockMvc.perform(post("/api/users")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    void create_shouldReturn400_whenPhoneNumberInvalid() throws Exception {

        UserCreateRequestDto request = new UserCreateRequestDto(
            1L, "John", "Doe", "12345", "123 Main St"
        );

        mockMvc.perform(post("/api/users")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(userService, never()).createUser(any(), any());
    }


    // ===================== GET BY ID =====================

    @Test
    void getById_shouldReturn200_whenAdmin() throws Exception {

        UserResponseDto response = new UserResponseDto(
            1L, "John", "Doe", "09171234567", "123 Main St", 1L
        );

        when(userService.getUserById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/users/1")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void getById_shouldReturn403_whenUserRole() throws Exception {

        mockMvc.perform(get("/api/users/1")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isForbidden());

        verify(userService, never()).getUserById(any());
    }

    @Test
    void getById_shouldReturn404_whenNotFound() throws Exception {

        when(userService.getUserById(1L))
            .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/users/1")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isNotFound());
    }

    @Test
    void getById_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isUnauthorized());
    }


    // ===================== GET ALL =====================

    @Test
    void getAll_shouldReturn200_whenAdmin() throws Exception {

        UserResponseDto response = new UserResponseDto(
            1L, "John", "Doe", "09171234567", "123 Main St", 1L
        );

        Page<UserResponseDto> page = new PageImpl<>(List.of(response));

        when(userService.getAllUsers(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/users")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].firstName").value("John"));
    }

    @Test
    void getAll_shouldReturn403_whenUserRole() throws Exception {

        mockMvc.perform(get("/api/users")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isForbidden());

        verify(userService, never()).getAllUsers(any());
    }

    @Test
    void getAll_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(get("/api/users"))
            .andExpect(status().isUnauthorized());
    }


    // ===================== UPDATE =====================

    @Test
    void update_shouldReturn200_whenOwner() throws Exception {

        UserUpdateRequestDto request = new UserUpdateRequestDto(
            "NewFirst", "NewLast", "09179999999", "New Address"
        );

        UserResponseDto response = new UserResponseDto(
            1L, "NewFirst", "NewLast", "09179999999", "New Address", 5L
        );

        when(userService.updateUser(eq(1L), any(UserUpdateRequestDto.class), eq(5L), eq(false)))
            .thenReturn(response);

        mockMvc.perform(put("/api/users/1")
                .with(authentication(userAuth(5L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("NewFirst"));
    }

    @Test
    void update_shouldReturn200_whenAdmin() throws Exception {

        UserUpdateRequestDto request = new UserUpdateRequestDto(
            "NewFirst", "NewLast", "09179999999", "New Address"
        );

        UserResponseDto response = new UserResponseDto(
            1L, "NewFirst", "NewLast", "09179999999", "New Address", 5L
        );

        when(userService.updateUser(eq(1L), any(UserUpdateRequestDto.class), eq(99L), eq(true)))
            .thenReturn(response);

        mockMvc.perform(put("/api/users/1")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
    }

    @Test
    void update_shouldReturn403_whenNotOwnerAndNotAdmin() throws Exception {

        UserUpdateRequestDto request = new UserUpdateRequestDto(
            "NewFirst", "NewLast", "09179999999", "New Address"
        );

        when(userService.updateUser(eq(1L), any(UserUpdateRequestDto.class), eq(999L), eq(false)))
            .thenThrow(new UnauthorizedActionException("You do not own this profile"));

        mockMvc.perform(put("/api/users/1")
                .with(authentication(userAuth(999L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    void update_shouldReturn401_whenUnauthenticated() throws Exception {

        UserUpdateRequestDto request = new UserUpdateRequestDto(
            "NewFirst", "NewLast", "09179999999", "New Address"
        );

        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void update_shouldReturn404_whenNotFound() throws Exception {

        UserUpdateRequestDto request = new UserUpdateRequestDto(
            "NewFirst", "NewLast", "09179999999", "New Address"
        );

        when(userService.updateUser(eq(1L), any(UserUpdateRequestDto.class), eq(5L), eq(false)))
            .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(put("/api/users/1")
                .with(authentication(userAuth(5L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void update_shouldReturn400_whenPhoneNumberInvalid() throws Exception {

        UserUpdateRequestDto request = new UserUpdateRequestDto(
            "NewFirst", "NewLast", "12345", "New Address"
        );

        mockMvc.perform(put("/api/users/1")
                .with(authentication(userAuth(5L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(userService, never()).updateUser(any(), any(), any(), anyBoolean());
    }


    // ===================== DELETE =====================

    @Test
    void delete_shouldReturn204_whenAdmin() throws Exception {

        doNothing().when(userService).deleteUser(1L, 99L);

        mockMvc.perform(delete("/api/users/1")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser(1L, 99L);
    }

    @Test
    void delete_shouldReturn403_whenUserRole() throws Exception {

        mockMvc.perform(delete("/api/users/1")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isForbidden());

        verify(userService, never()).deleteUser(any(), any());
    }

    @Test
    void delete_shouldReturn404_whenNotFound() throws Exception {

        doThrow(new ResourceNotFoundException("User not found"))
            .when(userService).deleteUser(1L, 99L);

        mockMvc.perform(delete("/api/users/1")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(delete("/api/users/1"))
            .andExpect(status().isUnauthorized());
    }
}