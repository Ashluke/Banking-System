package com.banking.system.controller;

import com.banking.system.dto.response.StockQuoteResponseDto;
import com.banking.system.services.StockService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/market")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/stocks")
    public List<StockQuoteResponseDto> getStocks(@RequestParam List<String> symbols) {
        return stockService.fetchQuotes(symbols);
    }
}