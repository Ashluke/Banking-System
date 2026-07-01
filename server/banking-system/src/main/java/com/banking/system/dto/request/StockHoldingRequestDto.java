package com.banking.system.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class StockHoldingRequestDto {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Symbol is required")
    @Size(max = 10, message = "Symbol must not exceed 10 characters")
    private String symbol;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than 0")
    @Digits(integer = 10, fraction = 6)
    private BigDecimal quantity;

    @NotNull(message = "Purchase price is required")
    @Positive(message = "Purchase price must be greater than 0")
    @Digits(integer = 10, fraction = 6)
    private BigDecimal purchasePrice;

    public StockHoldingRequestDto() {}

    public StockHoldingRequestDto(Long userId, String symbol, BigDecimal quantity, BigDecimal purchasePrice) {
        this.userId = userId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
    }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }

    public Long getUserId() { return userId; }
    public String getSymbol() { return symbol; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
}