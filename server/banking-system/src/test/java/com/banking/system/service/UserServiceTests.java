package com.banking.system.service;

import com.banking.system.dto.request.AdminUserUpdateRequestDto;
import com.banking.system.dto.request.UserCreateRequestDto;
import com.banking.system.dto.request.UserUpdateRequestDto;
import com.banking.system.dto.response.AuditLogResponseDto;
import com.banking.system.dto.response.UserRegisterResponseDto;
import com.banking.system.dto.response.UserResponseDto;
import com.banking.system.exception.DuplicateResourceException;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.exception.UnauthorizedActionException;
import com.banking.system.model.entities.AppUser;
import com.banking.system.model.entities.User;
import com.banking.system.model.enums.ActionType;
import com.banking.system.model.enums.Role;
import com.banking.system.repository.AppUserRepository;
import com.banking.system.repository.UserRepository;
import com.banking.system.services.AuditLogService;
import com.banking.system.services.UserService;

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
public class UserServiceTests {

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private UserService userService;


    // ===================== CREATE CUSTOMER =====================

    @Test
    void createCustomer_shouldCreateAppUserAndUser_whenEmailNotTaken() {

        when(appUserRepository.existsByEmail("john@example.com")).thenReturn(false);

        AppUser savedAppUser = mock(AppUser.class);
        when(savedAppUser.getId()).thenReturn(1L);
        when(savedAppUser.getId()).thenReturn(1L);
        when(savedAppUser.getEmail()).thenReturn("john@example.com");
        when(savedAppUser.getRole()).thenReturn(Role.USER);
        when(savedAppUser.getCreatedAt()).thenReturn(LocalDateTime.now());

        when(appUserRepository.save(any(AppUser.class))).thenReturn(savedAppUser);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        when(auditLogService.logAction(eq(99L), eq(1L), eq(ActionType.CREATE_USER)))
            .thenReturn(new AuditLogResponseDto(1L, 99L, 1L, ActionType.CREATE_USER, LocalDateTime.now()));

        UserCreateRequestDto request = new UserCreateRequestDto(
            "john@example.com", "password123", "John", "Doe", "09171234567", "123 Main St"
        );

        UserRegisterResponseDto result = userService.createCustomer(request, 99L);

        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals("john@example.com", result.getEmail());
        assertEquals(Role.USER, result.getRole());

        verify(appUserRepository, times(1)).save(any(AppUser.class));
        verify(userRepository, times(1)).save(any(User.class));
        verify(auditLogService, times(1)).logAction(eq(99L), eq(1L), eq(ActionType.CREATE_USER));
    }

    @Test
    void createCustomer_shouldThrowException_whenEmailAlreadyTaken() {

        when(appUserRepository.existsByEmail("john@example.com")).thenReturn(true);

        UserCreateRequestDto request = new UserCreateRequestDto(
            "john@example.com", "password123", "John", "Doe", "09171234567", "123 Main St"
        );

        assertThrows(DuplicateResourceException.class, () ->
            userService.createCustomer(request, 99L)
        );

        verify(appUserRepository, never()).save(any());
        verify(userRepository, never()).save(any());
        verify(auditLogService, never()).logAction(any(), any(), any());
    }


    // ===================== GET BY ID =====================

    @Test
    void getUserById_shouldReturnUser_whenFound() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(1L);

        User user = new User(1L, appUser, "John", "Doe", "09171234567", "123 Main St");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponseDto result = userService.getUserById(1L);

        assertEquals("John", result.getFirstName());
        assertEquals(1L, result.getAppUserId());
    }

    @Test
    void getUserById_shouldThrowException_whenNotFound() {

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            userService.getUserById(1L)
        );
    }


    // ===================== GET ALL =====================

    @Test
    void getAllUsers_shouldReturnPagedUsers() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(1L);

        User user = new User(1L, appUser, "John", "Doe", "09171234567", "123 Main St");

        when(userRepository.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(user)));

        Page<UserResponseDto> result = userService.getAllUsers(Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
    }


    // ===================== UPDATE USER (self-service: phone + address only) =====================

    @Test
    void updateUser_shouldModifyPhoneAndAddress_whenOwner() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(1L);

        User user = new User(1L, appUser, "John", "Doe", "09170000000", "Old Address");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        when(auditLogService.logAction(eq(1L), eq(1L), eq(ActionType.UPDATE_USER)))
            .thenReturn(new AuditLogResponseDto(1L, 1L, 1L, ActionType.UPDATE_USER, LocalDateTime.now()));

        UserUpdateRequestDto request = new UserUpdateRequestDto();
        request.setPhoneNumber("09179999999");
        request.setAddress("New Address");

        UserResponseDto result = userService.updateUser(1L, request, 1L, false);

        assertEquals("09179999999", result.getPhoneNumber());
        assertEquals("New Address", result.getAddress());
        // Names should remain unchanged since self-service only allows phone+address
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());

        verify(auditLogService, times(1)).logAction(eq(1L), eq(1L), eq(ActionType.UPDATE_USER));
    }

    @Test
    void updateUser_shouldThrowException_whenNotOwner() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(1L);

        User user = new User(1L, appUser, "John", "Doe", "09170000000", "Old Address");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserUpdateRequestDto request = new UserUpdateRequestDto();
        request.setPhoneNumber("09179999999");
        request.setAddress("New Address");

        assertThrows(UnauthorizedActionException.class, () ->
            userService.updateUser(1L, request, 999L, false)
        );

        verify(userRepository, never()).save(any());
        verify(auditLogService, never()).logAction(any(), any(), any());
    }

    @Test
    void updateUser_shouldThrowException_whenNotFound() {

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        UserUpdateRequestDto request = new UserUpdateRequestDto();
        request.setPhoneNumber("09179999999");

        assertThrows(ResourceNotFoundException.class, () ->
            userService.updateUser(1L, request, 99L, false)
        );

        verify(userRepository, never()).save(any());
        verify(auditLogService, never()).logAction(any(), any(), any());
    }


    // ===================== UPDATE USER BY ADMIN (all fields including name) =====================

    @Test
    void updateUserByAdmin_shouldModifyAllFields() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(1L);

        User user = new User(1L, appUser, "OldFirst", "OldLast", "09170000000", "Old Address");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        when(auditLogService.logAction(eq(99L), eq(1L), eq(ActionType.UPDATE_USER)))
            .thenReturn(new AuditLogResponseDto(1L, 99L, 1L, ActionType.UPDATE_USER, LocalDateTime.now()));

        AdminUserUpdateRequestDto request = new AdminUserUpdateRequestDto(
            "NewFirst", "NewLast", "09179999999", "New Address"
        );

        UserResponseDto result = userService.updateUserByAdmin(1L, request, 99L);

        assertEquals("NewFirst", result.getFirstName());
        assertEquals("NewLast", result.getLastName());
        assertEquals("09179999999", result.getPhoneNumber());
        assertEquals("New Address", result.getAddress());

        verify(auditLogService, times(1)).logAction(eq(99L), eq(1L), eq(ActionType.UPDATE_USER));
    }

    @Test
    void updateUserByAdmin_shouldThrowException_whenNotFound() {

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        AdminUserUpdateRequestDto request = new AdminUserUpdateRequestDto(
            "NewFirst", "NewLast", "09179999999", "New Address"
        );

        assertThrows(ResourceNotFoundException.class, () ->
            userService.updateUserByAdmin(1L, request, 99L)
        );

        verify(userRepository, never()).save(any());
        verify(auditLogService, never()).logAction(any(), any(), any());
    }


    // ===================== DELETE =====================

    @Test
    void deleteUser_shouldRemoveUser_andLogAction() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(1L);

        User user = new User(1L, appUser, "John", "Doe", "09171234567", "123 Main St");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(auditLogService.logAction(eq(99L), eq(1L), eq(ActionType.DELETE_USER)))
            .thenReturn(new AuditLogResponseDto(1L, 99L, 1L, ActionType.DELETE_USER, LocalDateTime.now()));

        userService.deleteUser(1L, 99L);

        verify(userRepository, times(1)).delete(user);
        verify(auditLogService, times(1)).logAction(eq(99L), eq(1L), eq(ActionType.DELETE_USER));
    }

    @Test
    void deleteUser_shouldThrowException_whenNotFound() {

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            userService.deleteUser(1L, 99L)
        );

        verify(userRepository, never()).delete(any());
        verify(auditLogService, never()).logAction(any(), any(), any());
    }
}