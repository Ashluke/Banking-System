package com.banking.system.model.entities;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.banking.system.model.enums.TransactionType;

import jakarta.persistence.*;

@Entity
@Table(name = "transactions")
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    @Column(name = "related_transaction_id")
    private Long relatedTransactionId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    public Transaction() {}

    public Transaction(BankAccount bankAccount, BigDecimal amount, TransactionType type) {
        this.bankAccount = bankAccount;
        this.amount = amount;
        this.type = type;
    }

    public Transaction(BankAccount bankAccount, Long relatedTransactionId, BigDecimal amount, TransactionType type) {
        this.bankAccount = bankAccount;
        this.relatedTransactionId = relatedTransactionId;
        this.amount = amount;
        this.type = type;
    }

    public void setBankAccount(BankAccount bankAccount) { this.bankAccount = bankAccount; }
    public void setRelatedTransactionId(Long relatedTransactionId) { this.relatedTransactionId = relatedTransactionId; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setType(TransactionType type) { this.type = type; }

    public Long getId() { return id; }
    public BankAccount getBankAccount() { return bankAccount; }
    public Long getRelatedTransactionId() { return relatedTransactionId; }
    public BigDecimal getAmount() { return amount; }
    public TransactionType getType() { return type; }
    public LocalDateTime getTimestamp() { return timestamp; }

}
