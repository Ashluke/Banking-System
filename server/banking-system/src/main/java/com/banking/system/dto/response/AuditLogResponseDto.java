package com.banking.system.dto.response;

import java.time.LocalDateTime;

import com.banking.system.model.enums.ActionType;

public class AuditLogResponseDto {
    
    private Long id;
    private Long adminId;
    private Long targetAppUserId;
    private ActionType action;
    private LocalDateTime performedAt;

    public AuditLogResponseDto() {}

    public AuditLogResponseDto(Long id, Long adminId, Long targetAppUserId, ActionType action, LocalDateTime performedAt) {
        this.id = id;
        this.adminId = adminId;
        this.targetAppUserId = targetAppUserId;
        this.action = action;
        this.performedAt = performedAt;
    }

    public void setId(Long id) { this.id = id; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }
    public void setTargetAppUserId(Long targetAppUserId) { this.targetAppUserId = targetAppUserId; }
    public void setAction(ActionType action) { this.action = action; }
    public void setPerformedAt(LocalDateTime performedAt) { this.performedAt = performedAt; }

    public Long getId() { return id; }
    public Long getAdminId() { return adminId; }
    public Long getTargetAppUserId() { return targetAppUserId; }
    public ActionType getAction() { return action; }
    public LocalDateTime getPerformedAt() { return performedAt; }
    
}
