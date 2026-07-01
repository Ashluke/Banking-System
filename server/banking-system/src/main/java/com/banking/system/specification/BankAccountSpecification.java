package com.banking.system.specification;

import com.banking.system.model.entities.BankAccount;
import com.banking.system.model.enums.AccountStatus;

import org.springframework.data.jpa.domain.Specification;

public class BankAccountSpecification {

    public static Specification<BankAccount> hasUserId(Long userId) {
        return (root, query, cb) -> userId == null ? null
            : cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<BankAccount> hasStatus(AccountStatus status) {
        return (root, query, cb) -> status == null ? null
            : cb.equal(root.get("status"), status);
    }
}