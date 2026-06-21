package com.banking.system.repository;

import com.banking.system.model.entities.AuditLog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAll(Pageable pageable);

    Page<AuditLog> findByTargetAppUser_Id(Long targetAppUserId, Pageable pageable);

    Page<AuditLog> findByAdmin_AppUser_Id(Long appUserId, Pageable pageable);
}
