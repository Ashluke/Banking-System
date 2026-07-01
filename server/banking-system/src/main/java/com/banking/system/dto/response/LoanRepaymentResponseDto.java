package com.banking.system.dto.response;

import com.banking.system.model.enums.RepaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class LoanRepaymentResponseDto {

    private Long id;
    private Long loanId;
    private int installmentNumber;
    private BigDecimal amount;
    private LocalDate dueDate;
    private LocalDateTime paidAt;
    private RepaymentStatus status;

    public LoanRepaymentResponseDto() {}

    public LoanRepaymentResponseDto(Long id, Long loanId, int installmentNumber,
            BigDecimal amount, LocalDate dueDate, LocalDateTime paidAt, RepaymentStatus status) {
        this.id = id;
        this.loanId = loanId;
        this.installmentNumber = installmentNumber;
        this.amount = amount;
        this.dueDate = dueDate;
        this.paidAt = paidAt;
        this.status = status;
    }

    public void setId(Long id) { this.id = id; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }
    public void setInstallmentNumber(int installmentNumber) { this.installmentNumber = installmentNumber; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public void setStatus(RepaymentStatus status) { this.status = status; }

    public Long getId() { return id; }
    public Long getLoanId() { return loanId; }
    public int getInstallmentNumber() { return installmentNumber; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public RepaymentStatus getStatus() { return status; }
}