package com.banking.system.controller;

import com.banking.system.dto.request.LoanRequestDto;
import com.banking.system.dto.response.LoanRepaymentResponseDto;
import com.banking.system.dto.response.LoanResponseDto;
import com.banking.system.security.SecurityUtil;
import com.banking.system.services.LoanService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/apply")
    @ResponseStatus(HttpStatus.CREATED)
    public LoanResponseDto apply(@Valid @RequestBody LoanRequestDto request) {
        Long appUserId = SecurityUtil.getCurrentUserId();
        return loanService.applyForLoan(request, appUserId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/approve")
    public LoanResponseDto approve(@PathVariable Long id) {
        return loanService.approveLoan(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/reject")
    public LoanResponseDto reject(@PathVariable Long id) {
        return loanService.rejectLoan(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/disburse")
    public LoanResponseDto disburse(@PathVariable Long id) {
        return loanService.disburseLoan(id);
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{id}/pay")
    public LoanRepaymentResponseDto pay(@PathVariable Long id) {
        Long appUserId = SecurityUtil.getCurrentUserId();
        return loanService.makeRepayment(id, appUserId);
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/{id}")
    public LoanResponseDto getById(@PathVariable Long id) {
        Long appUserId = SecurityUtil.getCurrentUserId();
        boolean isAdmin = SecurityUtil.isAdmin();
        return loanService.getById(id, appUserId, isAdmin);
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/users/{userId}")
    public Page<LoanResponseDto> getByUserId(@PathVariable Long userId, Pageable pageable) {
        Long appUserId = SecurityUtil.getCurrentUserId();
        boolean isAdmin = SecurityUtil.isAdmin();
        return loanService.getByUserId(userId, appUserId, isAdmin, pageable);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public Page<LoanResponseDto> getAll(Pageable pageable) {
        return loanService.getAll(pageable);
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/{id}/schedule")
    public List<LoanRepaymentResponseDto> getSchedule(@PathVariable Long id) {
        Long appUserId = SecurityUtil.getCurrentUserId();
        boolean isAdmin = SecurityUtil.isAdmin();
        return loanService.getRepaymentSchedule(id, appUserId, isAdmin);
    }
}