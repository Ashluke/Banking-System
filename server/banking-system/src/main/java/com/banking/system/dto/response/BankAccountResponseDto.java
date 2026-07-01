package com.banking.system.dto.response;

import java.math.BigDecimal;

import com.banking.system.model.enums.AccountStatus;

public class BankAccountResponseDto {
    
    private Long id;
    private BigDecimal balance;
    private AccountStatus status;
    private Long userId;
    private boolean isJoint;

    public BankAccountResponseDto() {}

    public BankAccountResponseDto(Long id, BigDecimal balance, AccountStatus status, Long userId, boolean isJoint) {
        this.id = id;
        this.balance = balance;
        this.status = status;
        this.userId = userId;
        this.isJoint = isJoint;
    }

    public void setId(Long id) { this.id = id; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public void setStatus(AccountStatus status) { this.status = status; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setIsJoint(boolean isJoint) { this.isJoint = isJoint; }

    public Long getId() { return id; }
    public BigDecimal getBalance() { return balance; }
    public AccountStatus getStatus() { return status; }
    public Long getUserId() { return userId; }
    public boolean getIsJoint() { return isJoint; }
}
