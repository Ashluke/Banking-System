package com.banking.system.specification;

import com.banking.system.model.entities.AuditLog;
import com.banking.system.model.enums.ActionType;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class AuditLogSpecification {

    public static Specification<AuditLog> hasAdminAppUserId(Long adminAppUserId) {
        return (root, query, cb) -> adminAppUserId == null ? null
            : cb.equal(root.get("admin").get("appUser").get("id"), adminAppUserId);
    }

    public static Specification<AuditLog> hasAction(ActionType action) {
        return (root, query, cb) -> action == null ? null
            : cb.equal(root.get("action"), action);
    }

    public static Specification<AuditLog> afterDate(LocalDateTime from) {
        return (root, query, cb) -> from == null ? null
            : cb.greaterThanOrEqualTo(root.get("performedAt"), from);
    }

    public static Specification<AuditLog> beforeDate(LocalDateTime to) {
        return (root, query, cb) -> to == null ? null
            : cb.lessThanOrEqualTo(root.get("performedAt"), to);
    }
}