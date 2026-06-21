package com.banking.system.service;

import com.banking.system.dto.request.UserCreateRequestDto;
import com.banking.system.dto.request.UserUpdateRequestDto;
import com.banking.system.dto.response.AuditLogResponseDto;
import com.banking.system.dto.response.UserResponseDto;
import com.banking.system.exception.DuplicateResourceException;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.model.entities.AppUser;
import com.banking.system.model.entities.User;
import com.banking.system.model.enums.ActionType;
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


    // Create user success
    @Test
    void createUser_shouldCreateUser_whenAppUserExists_andNoExistingProfile() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(1L);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(appUser));

        when(userRepository.existsByAppUser_Id(1L)).thenReturn(false);

        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        when(auditLogService.logAction(eq(99L), eq(1L), eq(ActionType.CREATE_USER)))
            .thenReturn(new AuditLogResponseDto(1L, 99L, 1L, ActionType.CREATE_USER, LocalDateTime.now()));

        UserCreateRequestDto request = new UserCreateRequestDto();
        request.setAppUserId(1L);
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPhoneNumber("09171234567");
        request.setAddress("123 Main St");

        UserResponseDto result = userService.createUser(request, 99L);

        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals(1L, result.getAppUserId());

        verify(userRepository, times(1)).save(any(User.class));
        verify(auditLogService, times(1))
            .logAction(eq(99L), eq(1L), eq(ActionType.CREATE_USER));
    }

    // Create user - AppUser not found
    @Test
    void createUser_shouldThrowException_whenAppUserNotFound() {

        when(appUserRepository.findById(1L)).thenReturn(Optional.empty());

        UserCreateRequestDto request = new UserCreateRequestDto();
        request.setAppUserId(1L);

        assertThrows(ResourceNotFoundException.class, () ->
            userService.createUser(request, 99L)
        );

        verify(userRepository, never()).save(any());
        verify(auditLogService, never()).logAction(any(), any(), any());
    }

    // Create user - profile already exists
    @Test
    void createUser_shouldThrowException_whenProfileAlreadyExists() {

        AppUser appUser = mock(AppUser.class);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(appUser));

        when(userRepository.existsByAppUser_Id(1L)).thenReturn(true);

        UserCreateRequestDto request = new UserCreateRequestDto();
        request.setAppUserId(1L);

        assertThrows(DuplicateResourceException.class, () ->
            userService.createUser(request, 99L)
        );

        verify(userRepository, never()).save(any());
        verify(auditLogService, never()).logAction(any(), any(), any());
    }

    // Get by id success
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

    // Get by id not found
    @Test
    void getUserById_shouldThrowException_whenNotFound() {

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            userService.getUserById(1L)
        );
    }

    // Get all
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

    // Update user success
    @Test
    void updateUser_shouldModifyUser_andLogAction() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(1L);

        User user = new User(1L, appUser, "OldFirst", "OldLast", "09170000000", "Old Address");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        when(auditLogService.logAction(eq(99L), eq(1L), eq(ActionType.UPDATE_USER)))
            .thenReturn(new AuditLogResponseDto(1L, 99L, 1L, ActionType.UPDATE_USER, LocalDateTime.now()));

        UserUpdateRequestDto request = new UserUpdateRequestDto();
        request.setFirstName("NewFirst");
        request.setLastName("NewLast");
        request.setPhoneNumber("09179999999");
        request.setAddress("New Address");

        UserResponseDto result = userService.updateUser(1L, request, 99L, false);

        assertEquals("NewFirst", result.getFirstName());
        assertEquals("NewLast", result.getLastName());
        assertEquals("09179999999", result.getPhoneNumber());
        assertEquals("New Address", result.getAddress());

        verify(auditLogService, times(1))
            .logAction(eq(99L), eq(1L), eq(ActionType.UPDATE_USER));
    }

    // Update user not found
    @Test
    void updateUser_shouldThrowException_whenNotFound() {

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        UserUpdateRequestDto request = new UserUpdateRequestDto();
        request.setFirstName("NewFirst");

        assertThrows(ResourceNotFoundException.class, () ->
            userService.updateUser(1L, request, 99L, false)
        );

        verify(userRepository, never()).save(any());
        verify(auditLogService, never()).logAction(any(), any(), any());
    }

    // Delete user success
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
        verify(auditLogService, times(1))
            .logAction(eq(99L), eq(1L), eq(ActionType.DELETE_USER));
    }

    // Delete user not found
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