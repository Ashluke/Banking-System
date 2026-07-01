package com.banking.system.dto.analytics;

public class CreditScoreRequest {

    private TransactionAnalyticsRequest transactionData;
    private AccountAnalyticsData accountData;

    public CreditScoreRequest() {}

    public CreditScoreRequest(TransactionAnalyticsRequest transactionData, AccountAnalyticsData accountData) {
        this.transactionData = transactionData;
        this.accountData = accountData;
    }

    public void setTransactionData(TransactionAnalyticsRequest transactionData) { this.transactionData = transactionData; }
    public void setAccountData(AccountAnalyticsData accountData) { this.accountData = accountData; }

    public TransactionAnalyticsRequest getTransactionData() { return transactionData; }
    public AccountAnalyticsData getAccountData() { return accountData; }
}
