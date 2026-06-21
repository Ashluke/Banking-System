package com.banking.system.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.banking.system.model.enums.TransactionType;

public class TransactionResponseDto {
    
    private Long id;
    private Long bankAccountId;
    private Long relatedTransactionId;
    private BigDecimal amount;
    private TransactionType type;
    private LocalDateTime timestamp;

    public TransactionResponseDto() {}

    public TransactionResponseDto(Long id, Long bankAccountId, BigDecimal amount, TransactionType type, LocalDateTime timestamp) {
        this.id = id;
        this.bankAccountId = bankAccountId;
        this.amount = amount;
        this.type = type;
        this.timestamp = timestamp;
    }

    public TransactionResponseDto(Long id, Long bankAccountId, Long relatedTransactionId, BigDecimal amount, TransactionType type, LocalDateTime timestamp) {
        this.id = id;
        this.bankAccountId = bankAccountId;
        this.relatedTransactionId = relatedTransactionId;
        this.amount = amount;
        this.type = type;
        this.timestamp = timestamp;
    }

    public void setId(Long id) { this.id = id; }
    public void setBankAccountId(Long bankAccountId) { this.bankAccountId = bankAccountId; }
    public void setRelatedTransactionId(Long relatedTransactionId) { this.relatedTransactionId = relatedTransactionId; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setType(TransactionType type) { this.type = type; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Long getId() { return id; }
    public Long getBankAccountId() { return bankAccountId; }
    public Long getRelatedTransactionId() { return relatedTransactionId; }
    public BigDecimal getAmount() { return amount; }
    public TransactionType getType() { return type; }
    public LocalDateTime getTimestamp() { return timestamp; }
    
}
