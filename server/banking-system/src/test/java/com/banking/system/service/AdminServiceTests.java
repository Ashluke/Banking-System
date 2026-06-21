package com.banking.system.service;

import com.banking.system.dto.request.AdminCreateRequestDto;
import com.banking.system.dto.request.AdminUpdateRequestDto;
import com.banking.system.dto.response.AdminResponseDto;
import com.banking.system.dto.response.AuditLogResponseDto;
import com.banking.system.exception.DuplicateResourceException;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.model.entities.Admin;
import com.banking.system.model.entities.AppUser;
import com.banking.system.model.enums.ActionType;
import com.banking.system.repository.AdminRepository;
import com.banking.system.repository.AppUserRepository;
import com.banking.system.services.AdminService;
import com.banking.system.services.AuditLogService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTests {

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private AdminService adminService;


    // Create admin success
    @Test
    void createAdmin_shouldCreateAdmin_whenAppUserExists_andNoExistingAdmin() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(1L);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(appUser));

        when(adminRepository.existsByAppUser_Id(1L)).thenReturn(false);

        when(adminRepository.save(any(Admin.class))).thenAnswer(i -> i.getArgument(0));

        when(auditLogService.logAction(eq(99L), eq(1L), eq(ActionType.CREATE_ADMIN)))
            .thenReturn(new AuditLogResponseDto(1L, 99L, 1L, ActionType.CREATE_ADMIN, LocalDateTime.now()));

        AdminCreateRequestDto request = new AdminCreateRequestDto(1L, "STAFF001", "John", "Doe");

        AdminResponseDto result = adminService.createAdmin(request, 99L);

        assertEquals("STAFF001", result.getStaffCode());
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());

        verify(adminRepository, times(1)).save(any(Admin.class));
        verify(auditLogService, times(1))
            .logAction(eq(99L), eq(1L), eq(ActionType.CREATE_ADMIN));
    }

    // Create admin - AppUser not found
    @Test
    void createAdmin_shouldThrowException_whenAppUserNotFound() {

        when(appUserRepository.findById(1L)).thenReturn(Optional.empty());

        AdminCreateRequestDto request = new AdminCreateRequestDto(1L, "STAFF001", "John", "Doe");

        assertThrows(ResourceNotFoundException.class, () ->
            adminService.createAdmin(request, 99L)
        );

        verify(adminRepository, never()).save(any());
        verify(auditLogService, never()).logAction(any(), any(), any());
    }

    // Create admin - already exists
    @Test
    void createAdmin_shouldThrowException_whenAdminProfileAlreadyExists() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(1L);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(appUser));

        when(adminRepository.existsByAppUser_Id(1L)).thenReturn(true);

        AdminCreateRequestDto request = new AdminCreateRequestDto(1L, "STAFF001", "John", "Doe");

        assertThrows(DuplicateResourceException.class, () ->
            adminService.createAdmin(request, 99L)
        );

        verify(adminRepository, never()).save(any());
        verify(auditLogService, never()).logAction(any(), any(), any());
    }

    // Get by id success
    @Test
    void getAdminById_shouldReturnAdmin_whenFound() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(1L);

        Admin admin = new Admin(appUser, "STAFF001", "John", "Doe");

        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));

        AdminResponseDto result = adminService.getAdminById(1L);

        assertEquals("STAFF001", result.getStaffCode());
        assertEquals(1L, result.getAppUserId());
    }

    // Get by id not found
    @Test
    void getAdminById_shouldThrowException_whenNotFound() {

        when(adminRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            adminService.getAdminById(1L)
        );
    }

    // Get all
    @Test
    void getAllAdmins_shouldReturnPagedAdmins() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(1L);

        Admin admin = new Admin(appUser, "STAFF001", "John", "Doe");

        when(adminRepository.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(admin)));

        Page<AdminResponseDto> result = adminService.getAllAdmins(Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
    }

    // Update admin success
    @Test
    void updateAdmin_shouldModifyAdmin_andLogAction() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(1L);

        Admin admin = new Admin(appUser, "OLD001", "OldFirst", "OldLast");

        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));

        when(adminRepository.save(any(Admin.class))).thenAnswer(i -> i.getArgument(0));

        when(auditLogService.logAction(eq(99L), eq(1L), eq(ActionType.UPDATE_ADMIN)))
            .thenReturn(new AuditLogResponseDto(1L, 99L, 1L, ActionType.UPDATE_ADMIN, LocalDateTime.now()));

        AdminUpdateRequestDto request = new AdminUpdateRequestDto("NEW001", "NewFirst", "NewLast");

        AdminResponseDto result = adminService.updateAdmin(1L, request, 99L);

        assertEquals("NEW001", result.getStaffCode());
        assertEquals("NewFirst", result.getFirstName());
        assertEquals("NewLast", result.getLastName());

        verify(auditLogService, times(1))
            .logAction(eq(99L), eq(1L), eq(ActionType.UPDATE_ADMIN));
    }

    // Update admin not found
    @Test
    void updateAdmin_shouldThrowException_whenNotFound() {

        when(adminRepository.findById(1L)).thenReturn(Optional.empty());

        AdminUpdateRequestDto request = new AdminUpdateRequestDto("NEW001", "NewFirst", "NewLast");

        assertThrows(ResourceNotFoundException.class, () ->
            adminService.updateAdmin(1L, request, 99L)
        );

        verify(adminRepository, never()).save(any());
        verify(auditLogService, never()).logAction(any(), any(), any());
    }

    // Delete admin success
    @Test
    void deleteAdmin_shouldRemoveAdmin_andLogAction() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(1L);

        Admin admin = new Admin(appUser, "STAFF001", "John", "Doe");

        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));

        when(auditLogService.logAction(eq(99L), eq(1L), eq(ActionType.DELETE_ADMIN)))
            .thenReturn(new AuditLogResponseDto(1L, 99L, 1L, ActionType.DELETE_ADMIN, LocalDateTime.now()));

        adminService.deleteAdmin(1L, 99L);

        verify(adminRepository, times(1)).delete(admin);
        verify(auditLogService, times(1))
            .logAction(eq(99L), eq(1L), eq(ActionType.DELETE_ADMIN));
    }

    // Delete admin not found
    @Test
    void deleteAdmin_shouldThrowException_whenNotFound() {

        when(adminRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            adminService.deleteAdmin(1L, 99L)
        );

        verify(adminRepository, never()).delete(any());
        verify(auditLogService, never()).logAction(any(), any(), any());
    }
}