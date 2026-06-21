package com.banking.system.services;

import com.banking.system.dto.request.AppUserLoginRequestDto;
import com.banking.system.dto.request.AppUserCreateRequestDto;
import com.banking.system.dto.response.AppUserResponseDto;
import com.banking.system.dto.response.AuthResponseDto;
import com.banking.system.dto.request.AppUserUpdateRequestDto;
import com.banking.system.exception.DuplicateResourceException;
import com.banking.system.exception.InvalidCredentialsException;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.exception.UnauthorizedActionException;
import com.banking.system.model.entities.AppUser;
import com.banking.system.model.enums.ActionType;
import com.banking.system.repository.AppUserRepository;
import com.banking.system.security.JWTService;
import com.banking.system.util.PasswordUtil;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AppUserService {
    
    private final AuditLogService auditLogService;
    private final AppUserRepository appUserRepository;
    private final JWTService jwtService;

    public AppUserService(AuditLogService auditLogService, AppUserRepository appUserRepository, JWTService jwtService) {
        this.auditLogService = auditLogService;
        this.appUserRepository = appUserRepository;
        this.jwtService = jwtService;
    }

    // Create
    public AppUserResponseDto register(AppUserCreateRequestDto request) {

        if (appUserRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        AppUser user = new AppUser();
        
        user.setEmail(request.getEmail());
        user.setPasswordHash(PasswordUtil.hashPassword(request.getPassword()));
        user.setRole(request.getRole());

        AppUser saved = appUserRepository.save(user);

        return mapToResponse(saved);
    }

    // Login
    public AuthResponseDto login(AppUserLoginRequestDto request) {
        
        AppUser user = appUserRepository.findByEmail(request.getEmail()).orElseThrow(() -> 
            new ResourceNotFoundException("User not found"));

        if (!PasswordUtil.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generatedToken(user.getId(), user.getEmail(), user.getRole());

        return new AuthResponseDto(token);
    }

    // Get by id
    public AppUserResponseDto getById(Long id) {

        AppUser user = appUserRepository.findById(id).orElseThrow(() -> 
            new ResourceNotFoundException("User not found"));

        return mapToResponse(user);
    }

    // Get all
    public Page<AppUserResponseDto> getAll(Pageable pageable) {

        return appUserRepository.findAll(pageable)
            .map(this::mapToResponse);
    }

    // Update
    public AppUserResponseDto update(Long id, AppUserUpdateRequestDto requestDto, Long appUserId, boolean isAdmin) {

        AppUser user = appUserRepository.findById(id).orElseThrow(() -> 
            new ResourceNotFoundException("User not found"));

        if (!isAdmin && !user.getId().equals(appUserId)) {
            throw new UnauthorizedActionException("You do not own this account");
        }

        user.setEmail(requestDto.getEmail());
        user.setPasswordHash(PasswordUtil.hashPassword(requestDto.getPassword()));

        AppUser updated = appUserRepository.save(user);

        auditLogService.logAction(
            appUserId, 
            id, 
            ActionType.UPDATE_APPUSER
        );

        return mapToResponse(updated);
    }

    // Delete by id
    public void deleteById(Long id) {

        AppUser user = appUserRepository.findById(id).orElseThrow(() -> 
            new ResourceNotFoundException("User not found"));

        appUserRepository.delete(user);
    }

    // Mapper
    private AppUserResponseDto mapToResponse(AppUser user) {

        return new AppUserResponseDto(
            user.getId(),
            user.getEmail(),
            user.getRole(),
            user.getCreatedAt()
        );
    }
}
