package com.banking.system.controller;

import com.banking.system.dto.request.JointAccountInviteRequestDto;
import com.banking.system.dto.response.JointAccountMemberResponseDto;
import com.banking.system.security.SecurityUtil;
import com.banking.system.services.JointAccountService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts/{accountId}/joint")
public class JointAccountController {

    private final JointAccountService jointAccountService;

    public JointAccountController(JointAccountService jointAccountService) {
        this.jointAccountService = jointAccountService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/invite")
    @ResponseStatus(HttpStatus.CREATED)
    public JointAccountMemberResponseDto addCoOwner(
            @PathVariable Long accountId,
            @Valid @RequestBody JointAccountInviteRequestDto request) {

        Long adminAppUserId = SecurityUtil.getCurrentUserId();
        return jointAccountService.addCoOwner(accountId, request, adminAppUserId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/remove")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeCoOwner(@PathVariable Long accountId) {
        Long adminAppUserId = SecurityUtil.getCurrentUserId();
        jointAccountService.removeCoOwner(accountId, adminAppUserId);
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/members")
    public List<JointAccountMemberResponseDto> getMembers(@PathVariable Long accountId) {
        Long appUserId = SecurityUtil.getCurrentUserId();
        boolean isAdmin = SecurityUtil.isAdmin();
        return jointAccountService.getMembers(accountId, appUserId, isAdmin);
    }
}