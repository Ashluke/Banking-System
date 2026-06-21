package com.banking.system.repository;

import com.banking.system.model.entities.Transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    Page<Transaction> findByBankAccount_Id(Long bankAccountId, Pageable pageable);

    Page<Transaction> findAll(Pageable pageable);
}
