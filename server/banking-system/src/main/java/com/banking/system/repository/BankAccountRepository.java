package com.banking.system.repository;

import com.banking.system.model.entities.BankAccount;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    
    long countByUser_Id(Long userId);

    Page<BankAccount> findByUser_Id(Long userId, Pageable pageable);

    Page<BankAccount> findAll(Pageable pageable);
}
