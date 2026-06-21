package com.banking.system.services;

import com.banking.system.dto.response.AuditLogResponseDto;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.model.entities.Admin;
import com.banking.system.model.entities.AppUser;
import com.banking.system.model.entities.AuditLog;
import com.banking.system.model.enums.ActionType;
import com.banking.system.repository.AdminRepository;
import com.banking.system.repository.AuditLogRepository;
import com.banking.system.repository.AppUserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {
    
    private final AuditLogRepository auditLogRepository;
    private final AdminRepository adminRepository;
    private final AppUserRepository appUserRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, AdminRepository adminRepository, AppUserRepository appUserRepository) {
        this.auditLogRepository = auditLogRepository;
        this.adminRepository = adminRepository;
        this.appUserRepository = appUserRepository;
    }

    // Create log
    public AuditLogResponseDto logAction(Long adminAppUserId, Long targetAppUserId, ActionType action) {

        Admin admin = adminRepository.findByAppUser_Id(adminAppUserId).orElseThrow(() -> 
            new ResourceNotFoundException("Admin not found"));

        AppUser targetAppUser = appUserRepository.findById(targetAppUserId).orElseThrow(() -> 
            new ResourceNotFoundException("User not found"));

        AuditLog auditLog = new AuditLog(admin, targetAppUser, action);

        AuditLog saved = auditLogRepository.save(auditLog);

        return mapToResponse(saved);
    }

    // Get log by id
    public AuditLogResponseDto getById(Long id) {
        
        AuditLog auditLog = auditLogRepository.findById(id).orElseThrow(() -> 
            new ResourceNotFoundException("Audit log not found"));

        return mapToResponse(auditLog);
    }

    // Get all logs
    public Page<AuditLogResponseDto> getAll(Pageable pageable) {

        return auditLogRepository.findAll(pageable)
            .map(this::mapToResponse);
    }

    // Get by admin
    public Page<AuditLogResponseDto> getByAdminId(Long adminAppUserId, Pageable pageable) {

        return auditLogRepository.findByAdmin_AppUser_Id(adminAppUserId, pageable)
            .map(this::mapToResponse);
    }

    // Get by target user
    public Page<AuditLogResponseDto> getByTargetAppUserId(Long targetAppUserId, Pageable pageable) {

        return auditLogRepository.findByTargetAppUser_Id(targetAppUserId, pageable)
            .map(this::mapToResponse);
    }

    // Mapper
    private AuditLogResponseDto mapToResponse(AuditLog auditLog) {

        return new AuditLogResponseDto(
            auditLog.getId(),
            auditLog.getAdmin().getId(),
            auditLog.getTargetAppUser().getId(),
            auditLog.getAction(),
            auditLog.getPerformedAt()
        );
    }
}
