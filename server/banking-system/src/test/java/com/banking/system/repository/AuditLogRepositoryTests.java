package com.banking.system.repository;

import com.banking.system.model.entities.Admin;
import com.banking.system.model.entities.AppUser;
import com.banking.system.model.entities.AuditLog;
import com.banking.system.model.enums.ActionType;
import com.banking.system.model.enums.Role;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class AuditLogRepositoryTests {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private AppUserRepository appUserRepository;


    private AppUser createAppUser(String email, Role role) {

        AppUser appUser = new AppUser();
        appUser.setEmail(email);
        appUser.setPasswordHash("hashed");
        appUser.setRole(role);

        return appUserRepository.save(appUser);
    }

    private Admin createAdmin(String email, String staffCode) {

        AppUser appUser = createAppUser(email, Role.ADMIN);

        Admin admin = new Admin(appUser, staffCode, "John", "Doe");

        return adminRepository.save(admin);
    }


    @Test
    void findByTargetAppUser_Id_shouldReturnLogs_forThatTarget() {

        Admin admin = createAdmin("admin1@example.com", "STAFF001");
        AppUser target = createAppUser("target1@example.com", Role.USER);
        AppUser otherTarget = createAppUser("target2@example.com", Role.USER);

        auditLogRepository.save(new AuditLog(admin, target, ActionType.CREATE_USER));
        auditLogRepository.save(new AuditLog(admin, target, ActionType.UPDATE_USER));
        auditLogRepository.save(new AuditLog(admin, otherTarget, ActionType.CREATE_USER));

        Page<AuditLog> result = auditLogRepository.findByTargetAppUser_Id(
            target.getId(), PageRequest.of(0, 10)
        );

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
            .extracting(AuditLog::getAction)
            .containsExactlyInAnyOrder(ActionType.CREATE_USER, ActionType.UPDATE_USER);
    }

    @Test
    void findByTargetAppUser_Id_shouldReturnEmptyPage_whenNoLogs() {

        AppUser target = createAppUser("target3@example.com", Role.USER);

        Page<AuditLog> result = auditLogRepository.findByTargetAppUser_Id(
            target.getId(), PageRequest.of(0, 10)
        );

        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void findByAdmin_AppUser_Id_shouldReturnLogs_forThatAdmin() {

        Admin admin = createAdmin("admin2@example.com", "STAFF002");
        Admin otherAdmin = createAdmin("admin3@example.com", "STAFF003");
        AppUser target = createAppUser("target4@example.com", Role.USER);

        auditLogRepository.save(new AuditLog(admin, target, ActionType.CREATE_USER));
        auditLogRepository.save(new AuditLog(admin, target, ActionType.DELETE_USER));
        auditLogRepository.save(new AuditLog(otherAdmin, target, ActionType.CREATE_USER));

        Page<AuditLog> result = auditLogRepository.findByAdmin_AppUser_Id(
            admin.getAppUser().getId(), PageRequest.of(0, 10)
        );

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
            .extracting(AuditLog::getAction)
            .containsExactlyInAnyOrder(ActionType.CREATE_USER, ActionType.DELETE_USER);
    }

    @Test
    void findByAdmin_AppUser_Id_shouldReturnEmptyPage_whenNoLogs() {

        Admin admin = createAdmin("admin4@example.com", "STAFF004");

        Page<AuditLog> result = auditLogRepository.findByAdmin_AppUser_Id(
            admin.getAppUser().getId(), PageRequest.of(0, 10)
        );

        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void save_shouldPersistAuditLog_withGeneratedIdAndTimestamp() {

        Admin admin = createAdmin("admin5@example.com", "STAFF005");
        AppUser target = createAppUser("target5@example.com", Role.USER);

        AuditLog auditLog = new AuditLog(admin, target, ActionType.FREEZE_ACCOUNT);

        AuditLog saved = auditLogRepository.save(auditLog);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPerformedAt()).isNotNull();
        assertThat(saved.getAdmin().getId()).isEqualTo(admin.getId());
        assertThat(saved.getTargetAppUser().getId()).isEqualTo(target.getId());
    }

    @Test
    void findAll_shouldReturnPagedAuditLogs() {

        Admin admin = createAdmin("admin6@example.com", "STAFF006");
        AppUser target = createAppUser("target6@example.com", Role.USER);

        auditLogRepository.save(new AuditLog(admin, target, ActionType.CREATE_ADMIN));
        auditLogRepository.save(new AuditLog(admin, target, ActionType.UPDATE_ADMIN));

        Page<AuditLog> result = auditLogRepository.findAll(PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }
}