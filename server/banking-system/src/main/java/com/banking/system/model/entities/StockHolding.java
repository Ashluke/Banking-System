package com.banking.system.model.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_holdings")
public class StockHolding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false, precision = 15, scale = 6)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 15, scale = 6)
    private BigDecimal purchasePrice;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime purchasedAt;

    public StockHolding() {}

    public StockHolding(User user, String symbol, BigDecimal quantity, BigDecimal purchasePrice) {
        this.user = user;
        this.symbol = symbol;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
    }

    public void setUser(User user) { this.user = user; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getSymbol() { return symbol; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public LocalDateTime getPurchasedAt() { return purchasedAt; }
}