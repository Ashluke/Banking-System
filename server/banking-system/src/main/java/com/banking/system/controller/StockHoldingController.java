package com.banking.system.controller;

import com.banking.system.dto.request.StockHoldingRequestDto;
import com.banking.system.dto.response.StockHoldingResponseDto;
import com.banking.system.security.SecurityUtil;
import com.banking.system.services.StockHoldingService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/holdings")
public class StockHoldingController {

    private final StockHoldingService stockHoldingService;

    public StockHoldingController(StockHoldingService stockHoldingService) {
        this.stockHoldingService = stockHoldingService;
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockHoldingResponseDto add(@Valid @RequestBody StockHoldingRequestDto request) {
        return stockHoldingService.addHolding(request);
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/users/{userId}")
    public Page<StockHoldingResponseDto> getByUserId(@PathVariable Long userId, Pageable pageable) {
        return stockHoldingService.getByUserId(userId, pageable);
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/{id}")
    public StockHoldingResponseDto getById(@PathVariable Long id) {

        Long appUserId = SecurityUtil.getCurrentUserId();

        return stockHoldingService.getById(id, appUserId);
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PutMapping("/{id}")
    public StockHoldingResponseDto update(@PathVariable Long id, @Valid @RequestBody StockHoldingRequestDto request) {
        return stockHoldingService.updateHolding(id, request);
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        stockHoldingService.deleteHolding(id);
    }
}