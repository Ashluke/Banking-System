package com.banking.system.dto.request;

import com.banking.system.model.enums.LoanType;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class LoanRequestDto {

    @NotNull(message = "Bank account ID is required")
    private Long bankAccountId;

    @NotNull(message = "Loan type is required")
    private LoanType type;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than 0")
    @DecimalMin(value = "40000", message = "Minimum loan amount is 40,000")
    @Digits(integer = 13, fraction = 2)
    private BigDecimal amount;

    @NotNull(message = "Term is required")
    @Min(value = 3, message = "Minimum term is 3 months")
    @Max(value = 60, message = "Maximum term is 60 months")
    private Integer termMonths;

    public LoanRequestDto() {}

    public LoanRequestDto(Long bankAccountId, LoanType type, BigDecimal amount, Integer termMonths) {
        this.bankAccountId = bankAccountId;
        this.type = type;
        this.amount = amount;
        this.termMonths = termMonths;
    }

    public void setBankAccountId(Long bankAccountId) { this.bankAccountId = bankAccountId; }
    public void setType(LoanType type) { this.type = type; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setTermMonths(Integer termMonths) { this.termMonths = termMonths; }

    public Long getBankAccountId() { return bankAccountId; }
    public LoanType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public Integer getTermMonths() { return termMonths; }
}