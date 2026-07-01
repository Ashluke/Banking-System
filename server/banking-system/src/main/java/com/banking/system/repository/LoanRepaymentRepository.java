package com.banking.system.repository;

import com.banking.system.model.entities.LoanRepayment;
import com.banking.system.model.enums.RepaymentStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, Long> {

    List<LoanRepayment> findByLoan_IdOrderByInstallmentNumberAsc(Long loanId);

    List<LoanRepayment> findByLoan_IdAndStatus(Long loanId, RepaymentStatus status);

    Optional<LoanRepayment> findFirstByLoan_IdAndStatusOrderByInstallmentNumberAsc(Long loanId, RepaymentStatus status);

    int countByLoan_IdAndStatus(Long loanId, RepaymentStatus status);

    List<LoanRepayment> findByStatusAndDueDateBefore(RepaymentStatus status, LocalDate date);
}