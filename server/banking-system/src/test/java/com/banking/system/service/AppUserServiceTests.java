package com.banking.system.service;

import com.banking.system.dto.request.AppUserCreateRequestDto;
import com.banking.system.dto.request.AppUserLoginRequestDto;
import com.banking.system.dto.request.AppUserUpdateRequestDto;
import com.banking.system.dto.response.AppUserResponseDto;
import com.banking.system.dto.response.AuditLogResponseDto;
import com.banking.system.dto.response.AuthResponseDto;
import com.banking.system.exception.DuplicateResourceException;
import com.banking.system.exception.InvalidCredentialsException;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.exception.UnauthorizedActionException;
import com.banking.system.model.entities.AppUser;
import com.banking.system.model.enums.ActionType;
import com.banking.system.model.enums.Role;
import com.banking.system.repository.AppUserRepository;
import com.banking.system.security.JWTService;
import com.banking.system.services.AppUserService;
import com.banking.system.services.AuditLogService;
import com.banking.system.util.PasswordUtil;

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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AppUserServiceTests {

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private JWTService jwtService;

    @InjectMocks
    private AppUserService appUserService;


    // Register success
    @Test
    void register_shouldCreateUser_whenEmailNotTaken() {

        when(appUserRepository.existsByEmail("test@example.com")).thenReturn(false);

        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        AppUserCreateRequestDto request = new AppUserCreateRequestDto(
            "test@example.com", "password123", Role.USER
        );

        AppUserResponseDto result = appUserService.register(request);

        assertEquals("test@example.com", result.getEmail());
        assertEquals(Role.USER, result.getRole());

        verify(appUserRepository, times(1)).save(any(AppUser.class));
    }

    // Register duplicate email
    @Test
    void register_shouldThrowException_whenEmailAlreadyExists() {

        when(appUserRepository.existsByEmail("test@example.com")).thenReturn(true);

        AppUserCreateRequestDto request = new AppUserCreateRequestDto(
            "test@example.com", "password123", Role.USER
        );

        assertThrows(DuplicateResourceException.class, () ->
            appUserService.register(request)
        );

        verify(appUserRepository, never()).save(any());
    }

    // Login success
    @Test
    void login_shouldReturnToken_whenCredentialsValid() {

        String rawPassword = "password123";
        String hashed = PasswordUtil.hashPassword(rawPassword);

        AppUser user = new AppUser();
        user.setEmail("test@example.com");
        user.setPasswordHash(hashed);
        user.setRole(Role.USER);

        when(appUserRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(user));

        when(jwtService.generatedToken(any(), eq("test@example.com"), eq(Role.USER)))
            .thenReturn("fake-jwt-token");

        AppUserLoginRequestDto request = new AppUserLoginRequestDto("test@example.com", rawPassword);

        AuthResponseDto result = appUserService.login(request);

        assertEquals("fake-jwt-token", result.getToken());
    }

    // Login user not found
    @Test
    void login_shouldThrowException_whenUserNotFound() {

        when(appUserRepository.findByEmail("missing@example.com"))
            .thenReturn(Optional.empty());

        AppUserLoginRequestDto request = new AppUserLoginRequestDto("missing@example.com", "password123");

        assertThrows(ResourceNotFoundException.class, () ->
            appUserService.login(request)
        );
    }

    // Login wrong password
    @Test
    void login_shouldThrowException_whenPasswordInvalid() {

        String hashed = PasswordUtil.hashPassword("correctPassword");

        AppUser user = new AppUser();
        user.setEmail("test@example.com");
        user.setPasswordHash(hashed);
        user.setRole(Role.USER);

        when(appUserRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(user));

        AppUserLoginRequestDto request = new AppUserLoginRequestDto("test@example.com", "wrongPassword");

        assertThrows(InvalidCredentialsException.class, () ->
            appUserService.login(request)
        );
    }

    // Get by id success
    @Test
    void getById_shouldReturnUser_whenFound() {

        AppUser user = new AppUser();
        user.setEmail("test@example.com");
        user.setRole(Role.USER);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));

        AppUserResponseDto result = appUserService.getById(1L);

        assertEquals("test@example.com", result.getEmail());
    }

    // Get by id not found
    @Test
    void getById_shouldThrowException_whenNotFound() {

        when(appUserRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            appUserService.getById(1L)
        );
    }

    // Get all
    @Test
    void getAll_shouldReturnPagedUsers() {

        AppUser user = new AppUser();
        user.setEmail("test@example.com");
        user.setRole(Role.USER);

        when(appUserRepository.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(user)));

        Page<AppUserResponseDto> result = appUserService.getAll(Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
    }

    // Update success - by admin
    @Test
    void update_shouldModifyUser_andLogAction_whenAdmin() {

        AppUser user = mock(AppUser.class);
        when(user.getEmail()).thenReturn("new@example.com");
        when(user.getRole()).thenReturn(Role.USER);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));

        when(appUserRepository.save(any(AppUser.class))).thenReturn(user);

        when(auditLogService.logAction(anyLong(), eq(1L), eq(ActionType.UPDATE_APPUSER)))
            .thenReturn(new AuditLogResponseDto(1L, 99L, 1L, ActionType.UPDATE_APPUSER, LocalDateTime.now()));

        AppUserUpdateRequestDto request = new AppUserUpdateRequestDto("new@example.com", "newPassword123");

        AppUserResponseDto result = appUserService.update(1L, request, 99L, true);

        assertEquals("new@example.com", result.getEmail());

        verify(auditLogService, times(1))
            .logAction(eq(99L), eq(1L), eq(ActionType.UPDATE_APPUSER));
    }

    // Update success - by owner (not admin)
    @Test
    void update_shouldModifyUser_andLogAction_whenOwner() {

        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(1L);
        when(user.getEmail()).thenReturn("new@example.com");
        when(user.getRole()).thenReturn(Role.USER);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));

        when(appUserRepository.save(any(AppUser.class))).thenReturn(user);

        when(auditLogService.logAction(anyLong(), eq(1L), eq(ActionType.UPDATE_APPUSER)))
            .thenReturn(new AuditLogResponseDto(1L, 1L, 1L, ActionType.UPDATE_APPUSER, LocalDateTime.now()));

        AppUserUpdateRequestDto request = new AppUserUpdateRequestDto("new@example.com", "newPassword123");

        AppUserResponseDto result = appUserService.update(1L, request, 1L, false);

        assertEquals("new@example.com", result.getEmail());

        verify(auditLogService, times(1))
            .logAction(eq(1L), eq(1L), eq(ActionType.UPDATE_APPUSER));
    }

    // Update - not owner, not admin
    @Test
    void update_shouldThrowException_whenNotOwnerAndNotAdmin() {

        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(1L);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));

        AppUserUpdateRequestDto request = new AppUserUpdateRequestDto("new@example.com", "newPassword123");

        assertThrows(UnauthorizedActionException.class, () ->
            appUserService.update(1L, request, 999L, false)
        );

        verify(appUserRepository, never()).save(any());
        verify(auditLogService, never()).logAction(any(), any(), any());
    }

    // Update not found
    @Test
    void update_shouldThrowException_whenUserNotFound() {

        when(appUserRepository.findById(1L)).thenReturn(Optional.empty());

        AppUserUpdateRequestDto request = new AppUserUpdateRequestDto("new@example.com", "newPassword123");

        assertThrows(ResourceNotFoundException.class, () ->
            appUserService.update(1L, request, 99L, true)
        );

        verify(appUserRepository, never()).save(any());
        verify(auditLogService, never()).logAction(any(), any(), any());
    }

    // Delete success
    @Test
    void deleteById_shouldRemoveUser_whenFound() {

        AppUser user = new AppUser();

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));

        appUserService.deleteById(1L);

        verify(appUserRepository, times(1)).delete(user);
    }

    // Delete not found
    @Test
    void deleteById_shouldThrowException_whenNotFound() {

        when(appUserRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            appUserService.deleteById(1L)
        );

        verify(appUserRepository, never()).delete(any());
    }

    // Update preserves role when admin
    @Test
    void update_shouldNotChangeRole_whenUserIsAdmin() {

        AppUser user = mock(AppUser.class);
        when(user.getEmail()).thenReturn("newadmin@example.com");
        when(user.getRole()).thenReturn(Role.ADMIN);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));

        when(appUserRepository.save(any(AppUser.class))).thenReturn(user);

        when(auditLogService.logAction(anyLong(), eq(1L), eq(ActionType.UPDATE_APPUSER)))
            .thenReturn(new AuditLogResponseDto(1L, 99L, 1L, ActionType.UPDATE_APPUSER, LocalDateTime.now()));

        AppUserUpdateRequestDto request = new AppUserUpdateRequestDto("newadmin@example.com", "newPassword123");

        AppUserResponseDto result = appUserService.update(1L, request, 99L, true);

        assertEquals("newadmin@example.com", result.getEmail());
        assertEquals(Role.ADMIN, result.getRole());

        verify(auditLogService, times(1))
            .logAction(eq(99L), eq(1L), eq(ActionType.UPDATE_APPUSER));
    }
}