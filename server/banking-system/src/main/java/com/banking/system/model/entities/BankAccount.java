package com.banking.system.model.entities;
import java.math.BigDecimal;

import com.banking.system.model.enums.AccountStatus;

import jakarta.persistence.*;

@Entity
@Table(name = "bank_accounts")
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Column(nullable = false)
    private boolean isJoint = false;

    public BankAccount() {}

    public BankAccount(User user, BigDecimal balance, AccountStatus status) {
        this.user = user;
        this.balance = balance;
        this.status = status;
        this.isJoint = false;
    }

    public BankAccount(User user, BigDecimal balance, AccountStatus status, boolean isJoint) {
        this.user = user;
        this.balance = balance;
        this.status = status;
        this.isJoint = isJoint;
    }

    public void setUser(User user) { this.user = user; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public void setStatus(AccountStatus status) { this.status = status; }
    public void setIsJoint(boolean isJoint) { this.isJoint = isJoint; }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public BigDecimal getBalance() { return balance; }
    public AccountStatus getStatus() { return status; }
    public boolean getIsJoint() { return isJoint; }

}
