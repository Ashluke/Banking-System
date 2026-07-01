package com.banking.system.controller;

import com.banking.system.dto.request.AdminCreateRequestDto;
import com.banking.system.dto.request.AdminUpdateRequestDto;
import com.banking.system.dto.response.AdminRegisterResponseDto;
import com.banking.system.dto.response.AdminResponseDto;
import com.banking.system.exception.DuplicateResourceException;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.model.enums.Role;
import com.banking.system.security.JWTService;
import com.banking.system.security.SecurityConfig;
import com.banking.system.services.AdminService;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
public class AdminControllerTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private AdminService adminService;

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
            appUserId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private UsernamePasswordAuthenticationToken adminAuth(Long appUserId) {
        return new UsernamePasswordAuthenticationToken(
            appUserId, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }


    // ===================== REGISTER (combined AppUser + Admin) =====================

    @Test
    void register_shouldReturn201_whenAdmin() throws Exception {

        AdminCreateRequestDto request = new AdminCreateRequestDto(
            "john@example.com", "password123", "STAFF001", "John", "Doe"
        );

        AdminRegisterResponseDto response = new AdminRegisterResponseDto(
            1L, "STAFF001", "John", "Doe", 1L, "john@example.com", Role.ADMIN, LocalDateTime.now()
        );

        when(adminService.createAdmin(any(AdminCreateRequestDto.class), eq(99L)))
            .thenReturn(response);

        mockMvc.perform(post("/api/admins/register")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.staffCode").value("STAFF001"))
            .andExpect(jsonPath("$.email").value("john@example.com"))
            .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void register_shouldReturn403_whenUserRole() throws Exception {

        AdminCreateRequestDto request = new AdminCreateRequestDto(
            "john@example.com", "password123", "STAFF001", "John", "Doe"
        );

        mockMvc.perform(post("/api/admins/register")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());

        verify(adminService, never()).createAdmin(any(), any());
    }

    @Test
    void register_shouldReturn401_whenUnauthenticated() throws Exception {

        AdminCreateRequestDto request = new AdminCreateRequestDto(
            "john@example.com", "password123", "STAFF001", "John", "Doe"
        );

        mockMvc.perform(post("/api/admins/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void register_shouldReturn409_whenEmailAlreadyTaken() throws Exception {

        AdminCreateRequestDto request = new AdminCreateRequestDto(
            "john@example.com", "password123", "STAFF001", "John", "Doe"
        );

        when(adminService.createAdmin(any(AdminCreateRequestDto.class), eq(99L)))
            .thenThrow(new DuplicateResourceException("Email already in use"));

        mockMvc.perform(post("/api/admins/register")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    void register_shouldReturn400_whenEmailMissing() throws Exception {

        AdminCreateRequestDto request = new AdminCreateRequestDto(
            "", "password123", "STAFF001", "John", "Doe"
        );

        mockMvc.perform(post("/api/admins/register")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(adminService, never()).createAdmin(any(), any());
    }

    @Test
    void register_shouldReturn400_whenStaffCodeMissing() throws Exception {

        AdminCreateRequestDto request = new AdminCreateRequestDto(
            "john@example.com", "password123", "", "John", "Doe"
        );

        mockMvc.perform(post("/api/admins/register")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(adminService, never()).createAdmin(any(), any());
    }


    // ===================== GET BY ID =====================

    @Test
    void getById_shouldReturn200_whenAdmin() throws Exception {

        AdminResponseDto response = new AdminResponseDto(1L, "STAFF001", "John", "Doe", 1L);

        when(adminService.getAdminById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/admins/1")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.staffCode").value("STAFF001"));
    }

    @Test
    void getById_shouldReturn403_whenUserRole() throws Exception {

        mockMvc.perform(get("/api/admins/1")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isForbidden());

        verify(adminService, never()).getAdminById(any());
    }

    @Test
    void getById_shouldReturn404_whenNotFound() throws Exception {

        when(adminService.getAdminById(1L))
            .thenThrow(new ResourceNotFoundException("Admin not found"));

        mockMvc.perform(get("/api/admins/1")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isNotFound());
    }

    @Test
    void getById_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(get("/api/admins/1"))
            .andExpect(status().isUnauthorized());
    }


    // ===================== GET ALL =====================

    @Test
    void getAll_shouldReturn200_whenAdmin() throws Exception {

        AdminResponseDto response = new AdminResponseDto(1L, "STAFF001", "John", "Doe", 1L);
        Page<AdminResponseDto> page = new PageImpl<>(List.of(response));

        when(adminService.getAllAdmins(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admins")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].staffCode").value("STAFF001"));
    }

    @Test
    void getAll_shouldReturn403_whenUserRole() throws Exception {

        mockMvc.perform(get("/api/admins")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isForbidden());

        verify(adminService, never()).getAllAdmins(any());
    }

    @Test
    void getAll_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(get("/api/admins"))
            .andExpect(status().isUnauthorized());
    }


    // ===================== UPDATE =====================

    @Test
    void update_shouldReturn200_whenAdmin() throws Exception {

        AdminUpdateRequestDto request = new AdminUpdateRequestDto("NEW001", "NewFirst", "NewLast");
        AdminResponseDto response = new AdminResponseDto(1L, "NEW001", "NewFirst", "NewLast", 1L);

        when(adminService.updateAdmin(eq(1L), any(AdminUpdateRequestDto.class), eq(99L)))
            .thenReturn(response);

        mockMvc.perform(put("/api/admins/1")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.staffCode").value("NEW001"));
    }

    @Test
    void update_shouldReturn403_whenUserRole() throws Exception {

        AdminUpdateRequestDto request = new AdminUpdateRequestDto("NEW001", "NewFirst", "NewLast");

        mockMvc.perform(put("/api/admins/1")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());

        verify(adminService, never()).updateAdmin(any(), any(), any());
    }

    @Test
    void update_shouldReturn404_whenNotFound() throws Exception {

        AdminUpdateRequestDto request = new AdminUpdateRequestDto("NEW001", "NewFirst", "NewLast");

        when(adminService.updateAdmin(eq(1L), any(AdminUpdateRequestDto.class), eq(99L)))
            .thenThrow(new ResourceNotFoundException("Admin not found"));

        mockMvc.perform(put("/api/admins/1")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void update_shouldReturn400_whenFirstNameMissing() throws Exception {

        AdminUpdateRequestDto request = new AdminUpdateRequestDto("NEW001", "", "NewLast");

        mockMvc.perform(put("/api/admins/1")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(adminService, never()).updateAdmin(any(), any(), any());
    }

    @Test
    void update_shouldReturn401_whenUnauthenticated() throws Exception {

        AdminUpdateRequestDto request = new AdminUpdateRequestDto("NEW001", "NewFirst", "NewLast");

        mockMvc.perform(put("/api/admins/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }


    // ===================== DELETE =====================

    @Test
    void delete_shouldReturn204_whenAdmin() throws Exception {

        doNothing().when(adminService).deleteAdmin(1L, 99L);

        mockMvc.perform(delete("/api/admins/1")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isNoContent());

        verify(adminService, times(1)).deleteAdmin(1L, 99L);
    }

    @Test
    void delete_shouldReturn403_whenUserRole() throws Exception {

        mockMvc.perform(delete("/api/admins/1")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isForbidden());

        verify(adminService, never()).deleteAdmin(any(), any());
    }

    @Test
    void delete_shouldReturn404_whenNotFound() throws Exception {

        doThrow(new ResourceNotFoundException("Admin not found"))
            .when(adminService).deleteAdmin(1L, 99L);

        mockMvc.perform(delete("/api/admins/1")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(delete("/api/admins/1"))
            .andExpect(status().isUnauthorized());
    }
}