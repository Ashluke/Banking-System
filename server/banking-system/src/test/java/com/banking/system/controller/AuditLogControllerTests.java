package com.banking.system.controller;

import com.banking.system.dto.response.AuditLogResponseDto;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.model.enums.ActionType;
import com.banking.system.security.JWTService;
import com.banking.system.security.SecurityConfig;
import com.banking.system.services.AuditLogService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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

@WebMvcTest(AuditLogController.class)
@Import(SecurityConfig.class)
public class AuditLogControllerTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogService auditLogService;

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


    // ===================== GET BY ID =====================

    @Test
    void getById_shouldReturn200_whenAdmin() throws Exception {

        AuditLogResponseDto response = new AuditLogResponseDto(
            1L, 1L, 2L, ActionType.CREATE_ADMIN, LocalDateTime.now()
        );

        when(auditLogService.getById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/audit-logs/1")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.action").value("CREATE_ADMIN"));
    }

    @Test
    void getById_shouldReturn403_whenUserRole() throws Exception {

        mockMvc.perform(get("/api/audit-logs/1")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isForbidden());

        verify(auditLogService, never()).getById(any());
    }

    @Test
    void getById_shouldReturn404_whenNotFound() throws Exception {

        when(auditLogService.getById(1L))
            .thenThrow(new ResourceNotFoundException("Audit log not found"));

        mockMvc.perform(get("/api/audit-logs/1")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isNotFound());
    }

    @Test
    void getById_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(get("/api/audit-logs/1"))
            .andExpect(status().isUnauthorized());
    }


    // ===================== GET ALL =====================

    @Test
    void getAll_shouldReturn200_whenAdmin() throws Exception {

        AuditLogResponseDto response = new AuditLogResponseDto(
            1L, 1L, 2L, ActionType.CREATE_ADMIN, LocalDateTime.now()
        );

        Page<AuditLogResponseDto> page = new PageImpl<>(List.of(response));

        when(auditLogService.getAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/audit-logs")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].action").value("CREATE_ADMIN"));
    }

    @Test
    void getAll_shouldReturn403_whenUserRole() throws Exception {

        mockMvc.perform(get("/api/audit-logs")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isForbidden());

        verify(auditLogService, never()).getAll(any());
    }

    @Test
    void getAll_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(get("/api/audit-logs"))
            .andExpect(status().isUnauthorized());
    }


    // ===================== GET BY ADMIN ID =====================

    @Test
    void getByAdminId_shouldReturn200_whenAdmin() throws Exception {

        AuditLogResponseDto response = new AuditLogResponseDto(
            1L, 1L, 2L, ActionType.CREATE_ADMIN, LocalDateTime.now()
        );

        Page<AuditLogResponseDto> page = new PageImpl<>(List.of(response));

        when(auditLogService.getByAdminId(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/audit-logs/admin/1")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].adminId").value(1L));
    }

    @Test
    void getByAdminId_shouldReturn403_whenUserRole() throws Exception {

        mockMvc.perform(get("/api/audit-logs/admin/1")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isForbidden());

        verify(auditLogService, never()).getByAdminId(any(), any());
    }

    @Test
    void getByAdminId_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(get("/api/audit-logs/admin/1"))
            .andExpect(status().isUnauthorized());
    }


    // ===================== GET BY TARGET APP USER ID =====================

    @Test
    void getByTargetAppUserId_shouldReturn200_whenAdmin() throws Exception {

        AuditLogResponseDto response = new AuditLogResponseDto(
            1L, 1L, 2L, ActionType.CREATE_ADMIN, LocalDateTime.now()
        );

        Page<AuditLogResponseDto> page = new PageImpl<>(List.of(response));

        when(auditLogService.getByTargetAppUserId(eq(2L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/audit-logs/user/2")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].targetAppUserId").value(2L));
    }

    @Test
    void getByTargetAppUserId_shouldReturn403_whenUserRole() throws Exception {

        mockMvc.perform(get("/api/audit-logs/user/2")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isForbidden());

        verify(auditLogService, never()).getByTargetAppUserId(any(), any());
    }

    @Test
    void getByTargetAppUserId_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(get("/api/audit-logs/user/2"))
            .andExpect(status().isUnauthorized());
    }
}