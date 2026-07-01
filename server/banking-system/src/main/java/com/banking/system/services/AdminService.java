package com.banking.system.services;

import com.banking.system.dto.request.AdminCreateRequestDto;
import com.banking.system.dto.request.AdminUpdateRequestDto;
import com.banking.system.dto.response.AdminRegisterResponseDto;
import com.banking.system.dto.response.AdminResponseDto;
import com.banking.system.exception.DuplicateResourceException;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.model.entities.Admin;
import com.banking.system.model.entities.AppUser;
import com.banking.system.model.enums.ActionType;
import com.banking.system.model.enums.Role;
import com.banking.system.repository.AdminRepository;
import com.banking.system.repository.AppUserRepository;
import com.banking.system.util.PasswordUtil;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    
    private final AuditLogService auditLogService;
    private final AdminRepository adminRepository;
    private final AppUserRepository appUserRepository;

    public AdminService(AuditLogService auditLogService, AdminRepository adminRepository, AppUserRepository appUserRepository) {
        this.auditLogService = auditLogService;
        this.adminRepository = adminRepository;
        this.appUserRepository = appUserRepository;
    }

    // Create admin (AppUser + Admin in one transaction)
    @Transactional
    public AdminRegisterResponseDto createAdmin(AdminCreateRequestDto request, Long superAdminAppUserId) {

        if (appUserRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        // Create AppUser
        AppUser appUser = new AppUser();
        appUser.setEmail(request.getEmail());
        appUser.setPasswordHash(PasswordUtil.hashPassword(request.getPassword()));
        appUser.setRole(Role.ADMIN);

        AppUser savedAppUser = appUserRepository.save(appUser);

        // Create Admin profile
        Admin admin = new Admin();
        admin.setAppUser(savedAppUser);
        admin.setStaffCode(request.getStaffCode());
        admin.setFirstName(request.getFirstName());
        admin.setLastName(request.getLastName());

        Admin savedAdmin = adminRepository.save(admin);

        auditLogService.logAction(
            superAdminAppUserId,
            savedAppUser.getId(),
            ActionType.CREATE_ADMIN
        );

        return mapToRegisterResponse(savedAdmin, savedAppUser);
    }

    // Get by id
    public AdminResponseDto getAdminById(Long id) {

        Admin admin = adminRepository.findById(id).orElseThrow(() -> 
            new ResourceNotFoundException("Admin not found"));

        return mapToResponse(admin);
    }

    // Get all
    public Page<AdminResponseDto> getAllAdmins(Pageable pageable) {

        return adminRepository.findAll(pageable)
            .map(this::mapToResponse);
    }

    // Update admin
    public AdminResponseDto updateAdmin(Long adminId, AdminUpdateRequestDto request, Long superAdminAppUserId) {

        Admin admin = adminRepository.findById(adminId).orElseThrow(() -> 
            new ResourceNotFoundException("Admin not found"));

        admin.setStaffCode(request.getStaffCode());
        admin.setFirstName(request.getFirstName());
        admin.setLastName(request.getLastName());

        Admin updated = adminRepository.save(admin);

        auditLogService.logAction(
            superAdminAppUserId,
            updated.getAppUser().getId(),
            ActionType.UPDATE_ADMIN
        );

        return mapToResponse(updated);
    }

    // Delete admin
    public void deleteAdmin(Long adminId, Long superAdminAppUserId) {

        Admin admin = adminRepository.findById(adminId).orElseThrow(() -> 
            new ResourceNotFoundException("Admin not found"));

        adminRepository.delete(admin);

        auditLogService.logAction(
            superAdminAppUserId,
            admin.getAppUser().getId(),
            ActionType.DELETE_ADMIN
        );
    }

    // Mappers
    private AdminResponseDto mapToResponse(Admin admin) {

        return new AdminResponseDto(
            admin.getId(),
            admin.getStaffCode(),
            admin.getFirstName(),
            admin.getLastName(),
            admin.getAppUser().getId()
        );
    }

    private AdminRegisterResponseDto mapToRegisterResponse(Admin admin, AppUser appUser) {

        return new AdminRegisterResponseDto(
            admin.getId(),
            admin.getStaffCode(),
            admin.getFirstName(),
            admin.getLastName(),
            appUser.getId(),
            appUser.getEmail(),
            appUser.getRole(),
            appUser.getCreatedAt()
        );
    }
}