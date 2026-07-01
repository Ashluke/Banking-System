package com.banking.system.dto.response;

import com.banking.system.model.enums.LoanStatus;
import com.banking.system.model.enums.LoanType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LoanResponseDto {

    private Long id;
    private Long userId;
    private Long bankAccountId;
    private LoanType type;
    private LoanStatus status;
    private BigDecimal amount;
    private BigDecimal interestRate;
    private int termMonths;
    private BigDecimal totalInterest;
    private BigDecimal monthlyPayment;
    private BigDecimal totalAmount;
    private int creditScoreAtApplication;
    private LocalDateTime approvedAt;
    private LocalDateTime disbursedAt;
    private LocalDateTime appliedAt;

    public LoanResponseDto() {}

    public LoanResponseDto(Long id, Long userId, Long bankAccountId, LoanType type, LoanStatus status,
            BigDecimal amount, BigDecimal interestRate, int termMonths, BigDecimal totalInterest,
            BigDecimal monthlyPayment, BigDecimal totalAmount, int creditScoreAtApplication,
            LocalDateTime approvedAt, LocalDateTime disbursedAt, LocalDateTime appliedAt) {
        this.id = id;
        this.userId = userId;
        this.bankAccountId = bankAccountId;
        this.type = type;
        this.status = status;
        this.amount = amount;
        this.interestRate = interestRate;
        this.termMonths = termMonths;
        this.totalInterest = totalInterest;
        this.monthlyPayment = monthlyPayment;
        this.totalAmount = totalAmount;
        this.creditScoreAtApplication = creditScoreAtApplication;
        this.approvedAt = approvedAt;
        this.disbursedAt = disbursedAt;
        this.appliedAt = appliedAt;
    }

    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setBankAccountId(Long bankAccountId) { this.bankAccountId = bankAccountId; }
    public void setType(LoanType type) { this.type = type; }
    public void setStatus(LoanStatus status) { this.status = status; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public void setTermMonths(int termMonths) { this.termMonths = termMonths; }
    public void setTotalInterest(BigDecimal totalInterest) { this.totalInterest = totalInterest; }
    public void setMonthlyPayment(BigDecimal monthlyPayment) { this.monthlyPayment = monthlyPayment; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public void setCreditScoreAtApplication(int creditScoreAtApplication) { this.creditScoreAtApplication = creditScoreAtApplication; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public void setDisbursedAt(LocalDateTime disbursedAt) { this.disbursedAt = disbursedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getBankAccountId() { return bankAccountId; }
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