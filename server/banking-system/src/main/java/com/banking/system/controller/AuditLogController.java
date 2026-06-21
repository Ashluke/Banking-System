package com.banking.system.controller;

import com.banking.system.dto.response.AuditLogResponseDto;
import com.banking.system.services.AuditLogService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/audit-logs")
public class AuditLogController {
    
    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public AuditLogResponseDto getById(@PathVariable Long id) {
        return auditLogService.getById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public Page<AuditLogResponseDto> getAll(Pageable pageable) {
        return auditLogService.getAll(pageable);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/{adminId}")
    public Page<AuditLogResponseDto> getByAdminId(@PathVariable Long adminId, Pageable pageable) {
        return auditLogService.getByAdminId(adminId, pageable);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/user/{userId}")
    public Page<AuditLogResponseDto> getByTargetAppUserId(@PathVariable Long userId, Pageable pageable) {
        return auditLogService.getByTargetAppUserId(userId, pageable);
    }
}
