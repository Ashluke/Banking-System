package com.banking.system.controller;

import com.banking.system.dto.request.DepositRequestDto;
import com.banking.system.dto.request.TransferRequestDto;
import com.banking.system.dto.request.WithdrawRequestDto;
import com.banking.system.dto.response.TransactionResponseDto;
import com.banking.system.security.SecurityUtil;
import com.banking.system.services.TransactionService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/withdraw")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponseDto withdraw(@Valid @RequestBody WithdrawRequestDto request) {
        return transactionService.withdraw(request);
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/deposit")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponseDto deposit(@Valid @RequestBody DepositRequestDto request) {
        return transactionService.deposit(request);
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponseDto transfer(@Valid @RequestBody TransferRequestDto request) {
        return transactionService.transfer(request);
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/account/{accountId}")
    public Page<TransactionResponseDto> getByAccountId(@PathVariable Long accountId, Pageable pageable) {

        Long appUserId = SecurityUtil.getCurrentUserId();
        boolean isAdmin = SecurityUtil.isAdmin();

        return transactionService.getByAccountId(accountId, appUserId, isAdmin, pageable);
    }
}
