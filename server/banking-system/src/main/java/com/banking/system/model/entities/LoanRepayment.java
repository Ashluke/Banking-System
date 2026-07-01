package com.banking.system.model.entities;

import com.banking.system.model.enums.RepaymentStatus;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_repayments")
public class LoanRepayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(nullable = false)
    private int installmentNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = true)
    private LocalDateTime paidAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepaymentStatus status;

    public LoanRepayment() {}

    public LoanRepayment(Loan loan, int installmentNumber, BigDecimal amount, LocalDate dueDate) {
        this.loan = loan;
        this.installmentNumber = installmentNumber;
        this.amount = amount;
        this.dueDate = dueDate;
        this.status = RepaymentStatus.PENDING;
    }

    public void setStatus(RepaymentStatus status) { this.status = status; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public Long getId() { return id; }
    public Loan getLoan() { return loan; }
    public int getInstallmentNumber() { return installmentNumber; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public RepaymentStatus getStatus() { return status; }
}