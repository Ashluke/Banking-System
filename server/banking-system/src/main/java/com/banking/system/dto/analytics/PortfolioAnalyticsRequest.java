package com.banking.system.dto.analytics;

import com.banking.system.dto.request.StockHoldingAnalyticsDto;

import java.util.List;

public class PortfolioAnalyticsRequest {

    private Long userId;
    private List<StockHoldingAnalyticsDto> holdings;

    public PortfolioAnalyticsRequest() {}

    public PortfolioAnalyticsRequest(Long userId, List<StockHoldingAnalyticsDto> holdings) {
        this.userId = userId;
        this.holdings = holdings;
    }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setHoldings(List<StockHoldingAnalyticsDto> holdings) { this.holdings = holdings; }

    public Long getUserId() { return userId; }
    public List<StockHoldingAnalyticsDto> getHoldings() { return holdings; }
}