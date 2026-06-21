package com.banking.system.controller;

import com.banking.system.dto.request.BankAccountCreateRequestDto;
import com.banking.system.dto.response.BankAccountResponseDto;
import com.banking.system.services.BankAccountService;
import com.banking.system.security.SecurityUtil;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class BankAccountController {
    
    private final BankAccountService bankAccountService;

    public BankAccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BankAccountResponseDto create(@Valid @RequestBody BankAccountCreateRequestDto request) {

        Long appUserId = SecurityUtil.getCurrentUserId();
        
        return bankAccountService.createAccount(request, appUserId);
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/{id}")
    public BankAccountResponseDto getById(@PathVariable Long id) {

        Long appUserId = SecurityUtil.getCurrentUserId();
        boolean isAdmin = SecurityUtil.isAdmin();


        return bankAccountService.getBankAccountById(id, appUserId, isAdmin);
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/users/{id}")
    public Page<BankAccountResponseDto> getByUserId(@PathVariable Long id, Pageable pageable) {
        return bankAccountService.getByUserId(id, pageable);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{accountId}/close")
    public BankAccountResponseDto closeAccount(@PathVariable Long accountId) {

        Long appUserId = SecurityUtil.getCurrentUserId();
        boolean isAdmin = SecurityUtil.isAdmin();

        return bankAccountService.closeAccount(accountId, appUserId, isAdmin);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{accountId}/freeze")
    public BankAccountResponseDto freezeAccount(@PathVariable Long accountId) {

        Long appUserId = SecurityUtil.getCurrentUserId();
        boolean isAdmin = SecurityUtil.isAdmin();

        return bankAccountService.freezeAccount(accountId, appUserId, isAdmin);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{accountId}/unfreeze")
    public BankAccountResponseDto unfreezeAccount(@PathVariable Long accountId) {

        Long appUserId = SecurityUtil.getCurrentUserId();
        boolean isAdmin = SecurityUtil.isAdmin();

        return bankAccountService.unfreezeAccount(accountId, appUserId, isAdmin);
    }
}
