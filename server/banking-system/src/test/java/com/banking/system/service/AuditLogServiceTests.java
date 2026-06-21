package com.banking.system.service;

import com.banking.system.dto.response.AuditLogResponseDto;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.model.entities.Admin;
import com.banking.system.model.entities.AppUser;
import com.banking.system.model.entities.AuditLog;
import com.banking.system.model.enums.ActionType;
import com.banking.system.repository.AdminRepository;
import com.banking.system.repository.AppUserRepository;
import com.banking.system.repository.AuditLogRepository;
import com.banking.system.services.AuditLogService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuditLogServiceTests {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private AuditLogService auditLogService;


    // Log action success
    @Test
    void logAction_shouldCreateAuditLog_whenAdminAndTargetExist() {

        Admin admin = mock(Admin.class);
        when(admin.getId()).thenReturn(1L);

        AppUser targetAppUser = mock(AppUser.class);
        when(targetAppUser.getId()).thenReturn(2L);

        when(adminRepository.findByAppUser_Id(10L)).thenReturn(Optional.of(admin));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(targetAppUser));

        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        AuditLogResponseDto result = auditLogService.logAction(10L, 2L, ActionType.CREATE_ADMIN);

        assertEquals(1L, result.getAdminId());
        assertEquals(2L, result.getTargetAppUserId());
        assertEquals(ActionType.CREATE_ADMIN, result.getAction());

        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    // Log action - admin not found
    @Test
    void logAction_shouldThrowException_whenAdminNotFound() {

        when(adminRepository.findByAppUser_Id(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            auditLogService.logAction(10L, 2L, ActionType.CREATE_ADMIN)
        );

        verify(auditLogRepository, never()).save(any());
    }

    // Log action - target app user not found
    @Test
    void logAction_shouldThrowException_whenTargetAppUserNotFound() {

        Admin admin = mock(Admin.class);

        when(adminRepository.findByAppUser_Id(10L)).thenReturn(Optional.of(admin));
        when(appUserRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            auditLogService.logAction(10L, 2L, ActionType.CREATE_ADMIN)
        );

        verify(auditLogRepository, never()).save(any());
    }

    // Get by id success
    @Test
    void getById_shouldReturnAuditLog_whenFound() {

        Admin admin = mock(Admin.class);
        when(admin.getId()).thenReturn(1L);

        AppUser targetAppUser = mock(AppUser.class);
        when(targetAppUser.getId()).thenReturn(2L);

        AuditLog auditLog = new AuditLog(admin, targetAppUser, ActionType.UPDATE_ADMIN);

        when(auditLogRepository.findById(5L)).thenReturn(Optional.of(auditLog));

        AuditLogResponseDto result = auditLogService.getById(5L);

        assertEquals(1L, result.getAdminId());
        assertEquals(2L, result.getTargetAppUserId());
        assertEquals(ActionType.UPDATE_ADMIN, result.getAction());
    }

    // Get by id not found
    @Test
    void getById_shouldThrowException_whenNotFound() {

        when(auditLogRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            auditLogService.getById(5L)
        );
    }

    // Get all
    @Test
    void getAll_shouldReturnPagedAuditLogs() {

        Admin admin = mock(Admin.class);
        when(admin.getId()).thenReturn(1L);

        AppUser targetAppUser = mock(AppUser.class);
        when(targetAppUser.getId()).thenReturn(2L);

        AuditLog auditLog = new AuditLog(admin, targetAppUser, ActionType.DELETE_ADMIN);

        when(auditLogRepository.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(auditLog)));

        Page<AuditLogResponseDto> result = auditLogService.getAll(Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
    }

    // Get by admin id
    @Test
    void getByAdminId_shouldReturnPagedAuditLogs() {

        Admin admin = mock(Admin.class);
        when(admin.getId()).thenReturn(1L);

        AppUser targetAppUser = mock(AppUser.class);
        when(targetAppUser.getId()).thenReturn(2L);

        AuditLog auditLog = new AuditLog(admin, targetAppUser, ActionType.UPDATE_ADMIN);

        when(auditLogRepository.findByAdmin_AppUser_Id(eq(10L), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(auditLog)));

        Page<AuditLogResponseDto> result = auditLogService.getByAdminId(10L, Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        verify(auditLogRepository, times(1))
            .findByAdmin_AppUser_Id(eq(10L), any(Pageable.class));
    }

    // Get by target app user id
    @Test
    void getByTargetAppUserId_shouldReturnPagedAuditLogs() {

        Admin admin = mock(Admin.class);
        when(admin.getId()).thenReturn(1L);

        AppUser targetAppUser = mock(AppUser.class);
        when(targetAppUser.getId()).thenReturn(2L);

        AuditLog auditLog = new AuditLog(admin, targetAppUser, ActionType.CREATE_ADMIN);

        when(auditLogRepository.findByTargetAppUser_Id(eq(2L), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(auditLog)));

        Page<AuditLogResponseDto> result = auditLogService.getByTargetAppUserId(2L, Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        verify(auditLogRepository, times(1))
            .findByTargetAppUser_Id(eq(2L), any(Pageable.class));
    }
}