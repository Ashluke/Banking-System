package com.banking.system.services;

import com.banking.system.dto.request.AdminUserUpdateRequestDto;
import com.banking.system.dto.request.UserCreateRequestDto;
import com.banking.system.dto.request.UserUpdateRequestDto;
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
import com.banking.system.util.PasswordUtil;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final AppUserRepository appUserRepository;

    public UserService(AuditLogService auditLogService, UserRepository userRepository, AppUserRepository appUserRepository) {
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
        this.appUserRepository = appUserRepository;
    }

    // Create customer (AppUser + User in one transaction)
    @Transactional
    public UserRegisterResponseDto createCustomer(UserCreateRequestDto request, Long appUserId) {

        if (appUserRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        AppUser appUser = new AppUser();
        appUser.setEmail(request.getEmail());
        appUser.setPasswordHash(PasswordUtil.hashPassword(request.getPassword()));
        appUser.setRole(Role.USER);

        AppUser savedAppUser = appUserRepository.save(appUser);

        User user = new User();
        user.setAppUser(savedAppUser);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());

        User savedUser = userRepository.save(user);

        auditLogService.logAction(
            appUserId,
            savedAppUser.getId(),
            ActionType.CREATE_USER
        );

        return mapToRegisterResponse(savedUser, savedAppUser);
    }

    // Get by id
    public UserResponseDto getUserById(Long id) {

        User user = userRepository.findById(id).orElseThrow(() -> 
            new ResourceNotFoundException("User not found"));

        return mapToResponse(user);
    }

    // Get all
    public Page<UserResponseDto> getAllUsers(Pageable pageable) {

        return userRepository.findAll(pageable)
            .map(this::mapToResponse);
    }

    // Update user (phone and address only)
    public UserResponseDto updateUser(Long userId, UserUpdateRequestDto request, Long appUserId, boolean isAdmin) {

        User user = userRepository.findById(userId).orElseThrow(() -> 
            new ResourceNotFoundException("User not found"));

        if (!isAdmin && !user.getAppUser().getId().equals(appUserId)) {
            throw new UnauthorizedActionException("You do not own this profile");
        }

        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());

        User updated = userRepository.save(user);

        auditLogService.logAction(
            appUserId,
            updated.getAppUser().getId(),
            ActionType.UPDATE_USER
        );

        return mapToResponse(updated);
    }

    // Update user by admin (full update including names)
    public UserResponseDto updateUserByAdmin(Long userId, AdminUserUpdateRequestDto request, Long appUserId) {

        User user = userRepository.findById(userId).orElseThrow(() -> 
            new ResourceNotFoundException("User not found"));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());

        User updated = userRepository.save(user);

        auditLogService.logAction(
            appUserId,
            updated.getAppUser().getId(),
            ActionType.UPDATE_USER
        );

        return mapToResponse(updated);
    }

    // Delete user
    public void deleteUser(Long userId, Long appUserId) {

        User user = userRepository.findById(userId).orElseThrow(() -> 
            new ResourceNotFoundException("User not found"));

        userRepository.delete(user);

        auditLogService.logAction(
            appUserId,
            user.getAppUser().getId(),
            ActionType.DELETE_USER
        );
    }

    // Mappers
    private UserResponseDto mapToResponse(User user) {

        return new UserResponseDto(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getPhoneNumber(),
            user.getAddress(),
            user.getAppUser().getId()
        );
    }

    private UserRegisterResponseDto mapToRegisterResponse(User user, AppUser appUser) {

        return new UserRegisterResponseDto(
            user.getId(),
            appUser.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getPhoneNumber(),
            user.getAddress(),
            appUser.getId(),
            appUser.getRole(),
            appUser.getCreatedAt()
        );
    }
}