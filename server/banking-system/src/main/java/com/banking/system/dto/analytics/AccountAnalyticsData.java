package com.banking.system.dto.analytics;

public class AccountAnalyticsData {

    private Long id;
    private Long userId;
    private double balance;
    private String status;
    private String createdAt;

    public AccountAnalyticsData() {}

    public AccountAnalyticsData(Long id, Long userId, double balance, String status, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.balance = balance;
        this.status = status;
        this.createdAt = createdAt;
    }

    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setBalance(double balance) { this.balance = balance; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public double getBalance() { return balance; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
}