package com.banking.system.repository;

import com.banking.system.model.entities.Transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {

    Page<Transaction> findByBankAccount_Id(Long bankAccountId, Pageable pageable);

    Page<Transaction> findAll(Pageable pageable);

    Optional<Transaction> findTopByBankAccount_IdOrderByTimestampDesc(Long bankAccountId);
}