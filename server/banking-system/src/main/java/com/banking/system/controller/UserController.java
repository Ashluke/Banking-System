package com.banking.system.controller;

import com.banking.system.dto.request.UserCreateRequestDto;
import com.banking.system.dto.request.UserUpdateRequestDto;
import com.banking.system.dto.response.UserResponseDto;
import com.banking.system.security.SecurityUtil;
import com.banking.system.services.UserService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDto create(@Valid @RequestBody UserCreateRequestDto request) {

        Long appUserId = SecurityUtil.getCurrentUserId();

        return userService.createUser(request, appUserId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public UserResponseDto getById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public Page<UserResponseDto> getAll(Pageable pageable) {
        return userService.getAllUsers(pageable);
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PutMapping("/{id}")
    public UserResponseDto update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequestDto request) {

        Long appUserId = SecurityUtil.getCurrentUserId();
        boolean isAdmin = SecurityUtil.isAdmin();

        return userService.updateUser(id, request, appUserId, isAdmin);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {

        Long appUserId = SecurityUtil.getCurrentUserId();

        userService.deleteUser(id, appUserId);
    }
}
