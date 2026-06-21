package com.banking.system.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;

public class DepositRequestDto {
    
    @NotNull
    private Long accountId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than 0")
    @Digits(integer = 12, fraction = 2)
    private BigDecimal amount;

    public DepositRequestDto() {}

    public DepositRequestDto(Long accountId, BigDecimal amount) {
        this.accountId = accountId;
        this.amount = amount;
    }

    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Long getAccountId() { return accountId; }
    public BigDecimal getAmount() { return amount; }

}
