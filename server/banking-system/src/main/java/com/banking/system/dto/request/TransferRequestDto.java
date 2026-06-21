package com.banking.system.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;

public class TransferRequestDto {
    
    @NotNull(message = "From account is required")
    private Long fromAccountId;

    @NotNull(message = "To account is required")
    private Long toAccountId;

    @AssertTrue(message = "From and To accounts cannot be the same")
    public boolean isValidTransfer() {
        if (fromAccountId == null || toAccountId == null) {
            return true;
        }
        
        return !fromAccountId.equals(toAccountId);
    }

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than 0")
    @Digits(integer = 12, fraction = 2)
    private BigDecimal amount;

    public TransferRequestDto() {}

    public TransferRequestDto(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
    }

    public void setFromAccountId(Long fromAccountId) { this.fromAccountId = fromAccountId; }
    public void setToAccountId(Long toAccountId) { this.toAccountId = toAccountId; }
    public void setAmount(BigDecimal amount) { this.amount= amount; }

    public Long getFromAccountId() { return fromAccountId; }
    public Long getToAccountId() { return toAccountId; }
    public BigDecimal getAmount() { return amount; }
}
