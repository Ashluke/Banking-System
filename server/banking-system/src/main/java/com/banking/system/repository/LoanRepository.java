package com.banking.system.repository;

import com.banking.system.model.entities.Loan;
import com.banking.system.model.enums.LoanStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    Page<Loan> findByUser_Id(Long userId, Pageable pageable);

    Page<Loan> findByStatus(LoanStatus status, Pageable pageable);

    List<Loan> findByStatus(LoanStatus status);

    boolean existsByUser_IdAndStatusIn(Long userId, List<LoanStatus> statuses);
}