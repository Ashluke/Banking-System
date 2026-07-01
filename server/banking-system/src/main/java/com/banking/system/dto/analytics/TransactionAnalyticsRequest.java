package com.banking.system.dto.analytics;

import com.banking.system.dto.response.TransactionResponseDto;

import java.util.List;

public class TransactionAnalyticsRequest {

    private Long userId;
    private double currentBalance;
    private List<TransactionResponseDto> transactions;

    public TransactionAnalyticsRequest() {}

    public TransactionAnalyticsRequest(Long userId, double currentBalance, List<TransactionResponseDto> transactions) {
        this.userId = userId;
        this.currentBalance = currentBalance;
        this.transactions = transactions;
    }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setCurrentBalance(double currentBalance) { this.currentBalance = currentBalance; }
    public void setTransactions(List<TransactionResponseDto> transactions) { this.transactions = transactions; }

    public Long getUserId() { return userId; }
    public double getCurrentBalance() { return currentBalance; }
    public List<TransactionResponseDto> getTransactions() { return transactions; }
}



// ── Credit score request ──────────────────────────────────────────────────────



// ── Portfolio request ─────────────────────────────────────────────────────────

