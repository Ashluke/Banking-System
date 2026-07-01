package com.banking.system.repository;

import com.banking.system.model.entities.BankAccount;
import com.banking.system.model.enums.AccountStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long>,
        JpaSpecificationExecutor<BankAccount> {

    long countByUser_Id(Long userId);

    Page<BankAccount> findByUser_Id(Long userId, Pageable pageable);

    Page<BankAccount> findAll(Pageable pageable);

    List<BankAccount> findByStatus(AccountStatus status);
}