package com.banking.system.model.entities;

import com.banking.system.model.enums.LoanStatus;
import com.banking.system.model.enums.LoanType;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate;

    @Column(nullable = false)
    private int termMonths;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalInterest;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyPayment;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private int creditScoreAtApplication;

    @Column(nullable = true)
    private LocalDateTime approvedAt;

    @Column(nullable = true)
    private LocalDateTime disbursedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    public Loan() {}

    public Loan(User user, BankAccount bankAccount, LoanType type, BigDecimal amount,
            BigDecimal interestRate, int termMonths, BigDecimal totalInterest,
            BigDecimal monthlyPayment, BigDecimal totalAmount, int creditScoreAtApplication) {
        this.user = user;
        this.bankAccount = bankAccount;
        this.type = type;
        this.status = LoanStatus.PENDING;
        this.amount = amount;
        this.interestRate = interestRate;
        this.termMonths = termMonths;
        this.totalInterest = totalInterest;
        this.monthlyPayment = monthlyPayment;
        this.totalAmount = totalAmount;
        this.creditScoreAtApplication = creditScoreAtApplication;
    }

    public void setStatus(LoanStatus status) { this.status = status; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public void setDisbursedAt(LocalDateTime disbursedAt) { this.disbursedAt = disbursedAt; }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public BankAccount getBankAccount() { return bankAccount; }
    public LoanType getType() { return type; }
    public LoanStatus getStatus() { return status; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getInterestRate() { return interestRate; }
    public int getTermMonths() { return termMonths; }
    public BigDecimal getTotalInterest() { return totalInterest; }
    public BigDecimal getMonthlyPayment() { return monthlyPayment; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public int getCreditScoreAtApplication() { return creditScoreAtApplication; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public LocalDateTime getDisbursedAt() { return disbursedAt; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
}