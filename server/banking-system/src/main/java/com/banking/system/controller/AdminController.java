package com.banking.system.controller;

import com.banking.system.dto.request.AdminCreateRequestDto;
import com.banking.system.dto.request.AdminUpdateRequestDto;
import com.banking.system.dto.response.AdminRegisterResponseDto;
import com.banking.system.dto.response.AdminResponseDto;
import com.banking.system.security.SecurityUtil;
import com.banking.system.services.AdminService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admins")
public class AdminController {
    
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminRegisterResponseDto register(@Valid @RequestBody AdminCreateRequestDto request) {

        Long appUserId = SecurityUtil.getCurrentUserId();

        return adminService.createAdmin(request, appUserId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public AdminResponseDto getById(@PathVariable Long id) {
        return adminService.getAdminById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public Page<AdminResponseDto> getAll(Pageable pageable) {
        return adminService.getAllAdmins(pageable);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public AdminResponseDto update(@PathVariable Long id, @Valid @RequestBody AdminUpdateRequestDto request) {

        Long appUserId = SecurityUtil.getCurrentUserId();

        return adminService.updateAdmin(id, request, appUserId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {

        Long appUserId = SecurityUtil.getCurrentUserId();

        adminService.deleteAdmin(id, appUserId);
    }
}