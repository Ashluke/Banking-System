package com.banking.system.dto.response;

public class StockQuoteResponseDto {

    private String symbol;
    private double currentPrice;
    private double change;
    private double percentChange;
    private double highPrice;
    private double lowPrice;
    private double openPrice;
    private double previousClose;

    public StockQuoteResponseDto() {}

    public StockQuoteResponseDto(String symbol, double currentPrice, double change, double percentChange,
            double highPrice, double lowPrice, double openPrice, double previousClose) {
        this.symbol = symbol;
        this.currentPrice = currentPrice;
        this.change = change;
        this.percentChange = percentChange;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.openPrice = openPrice;
        this.previousClose = previousClose;
    }

    public void setSymbol(String symbol) { this.symbol = symbol; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public void setChange(double change) { this.change = change; }
    public void setPercentChange(double percentChange) { this.percentChange = percentChange; }
    public void setHighPrice(double highPrice) { this.highPrice = highPrice; }
    public void setLowPrice(double lowPrice) { this.lowPrice = lowPrice; }
    public void setOpenPrice(double openPrice) { this.openPrice = openPrice; }
    public void setPreviousClose(double previousClose) { this.previousClose = previousClose; }

    public String getSymbol() { return symbol; }
    public double getCurrentPrice() { return currentPrice; }
    public double getChange() { return change; }
    public double getPercentChange() { return percentChange; }
    public double getHighPrice() { return highPrice; }
    public double getLowPrice() { return lowPrice; }
    public double getOpenPrice() { return openPrice; }
    public double getPreviousClose() { return previousClose; }
}