package com.banking.system.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockHoldingResponseDto {

    private Long id;
    private Long userId;
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal purchasePrice;
    private LocalDateTime purchasedAt;

    public StockHoldingResponseDto() {}

    public StockHoldingResponseDto(Long id, Long userId, String symbol,
            BigDecimal quantity, BigDecimal purchasePrice, LocalDateTime purchasedAt) {
        this.id = id;
        this.userId = userId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
        this.purchasedAt = purchasedAt;
    }

    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
    public void setPurchasedAt(LocalDateTime purchasedAt) { this.purchasedAt = purchasedAt; }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getSymbol() { return symbol; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public LocalDateTime getPurchasedAt() { return purchasedAt; }
}