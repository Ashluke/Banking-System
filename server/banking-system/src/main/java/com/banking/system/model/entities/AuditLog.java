package com.banking.system.model.entities;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.banking.system.model.enums.ActionType;

import jakarta.persistence.*;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    @ManyToOne
    @JoinColumn(name = "target_app_user_id", nullable = false)
    private AppUser targetAppUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType action;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime performedAt;

    public AuditLog() {}

    public AuditLog(Admin admin, AppUser targetAppUser, ActionType action) {
        this.admin = admin;
        this.targetAppUser = targetAppUser;
        this.action = action;
    }

    public void setAdmin(Admin admin) { this.admin = admin; }
    public void setTargetAppUser(AppUser targetAppUser) { this.targetAppUser = targetAppUser; }
    public void setAction(ActionType action) { this.action = action; }

    public Long getId() { return id; }
    public Admin getAdmin() { return admin; }
    public AppUser getTargetAppUser() { return targetAppUser; }
    public ActionType getAction() { return action; }
    public LocalDateTime getPerformedAt() { return performedAt; }

}