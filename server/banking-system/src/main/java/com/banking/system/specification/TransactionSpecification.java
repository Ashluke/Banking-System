package com.banking.system.specification;

import com.banking.system.model.entities.Transaction;
import com.banking.system.model.enums.TransactionType;

import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionSpecification {

    public static Specification<Transaction> hasAccountId(Long accountId) {
        return (root, query, cb) -> accountId == null ? null
            : cb.equal(root.get("bankAccount").get("id"), accountId);
    }

    public static Specification<Transaction> hasType(TransactionType type) {
        return (root, query, cb) -> type == null ? null
            : cb.equal(root.get("type"), type);
    }

    public static Specification<Transaction> afterDate(LocalDateTime from) {
        return (root, query, cb) -> from == null ? null
            : cb.greaterThanOrEqualTo(root.get("timestamp"), from);
    }

    public static Specification<Transaction> beforeDate(LocalDateTime to) {
        return (root, query, cb) -> to == null ? null
            : cb.lessThanOrEqualTo(root.get("timestamp"), to);
    }

    public static Specification<Transaction> minAmount(BigDecimal min) {
        return (root, query, cb) -> min == null ? null
            : cb.greaterThanOrEqualTo(root.get("amount"), min);
    }

    public static Specification<Transaction> maxAmount(BigDecimal max) {
        return (root, query, cb) -> max == null ? null
            : cb.lessThanOrEqualTo(root.get("amount"), max);
    }
}