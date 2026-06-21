package com.banking.system.controller;

import com.banking.system.dto.request.AppUserCreateRequestDto;
import com.banking.system.dto.request.AppUserLoginRequestDto;
import com.banking.system.dto.request.AppUserUpdateRequestDto;
import com.banking.system.dto.response.AppUserResponseDto;
import com.banking.system.dto.response.AuthResponseDto;
import com.banking.system.security.SecurityUtil;
import com.banking.system.services.AppUserService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/app-users")
public class AppUserController {
    
    private final AppUserService appUserService;

    public AppUserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AppUserResponseDto register(@Valid @RequestBody AppUserCreateRequestDto request) {
        return appUserService.register(request);
    }

    @PostMapping("/login")
    public AuthResponseDto login(@Valid @RequestBody AppUserLoginRequestDto request) {
        return appUserService.login(request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public AppUserResponseDto getById(@PathVariable Long id) {
        return appUserService.getById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public Page<AppUserResponseDto> getAll(Pageable pageable) {
        return appUserService.getAll(pageable);
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PutMapping("/{id}")
    public AppUserResponseDto update(@PathVariable Long id, @Valid @RequestBody AppUserUpdateRequestDto request) {

        Long appUserId = SecurityUtil.getCurrentUserId();
        boolean isAdmin = SecurityUtil.isAdmin();

        return appUserService.update(id, request, appUserId, isAdmin);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        appUserService.deleteById(id);
    }
}