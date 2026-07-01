package com.banking.system.dto.response;

import java.time.LocalDateTime;

import com.banking.system.model.enums.Role;

public class AdminRegisterResponseDto {

    private Long id;
    private String staffCode;
    private String firstName;
    private String lastName;
    private Long appUserId;
    private String email;
    private Role role;
    private LocalDateTime createdAt;

    public AdminRegisterResponseDto() {}

    public AdminRegisterResponseDto(Long id, String staffCode, String firstName, String lastName,
            Long appUserId, String email, Role role, LocalDateTime createdAt) {
        this.id = id;
        this.staffCode = staffCode;
        this.firstName = firstName;
        this.lastName = lastName;
        this.appUserId = appUserId;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
    }

    public void setId(Long id) { this.id = id; }
    public void setStaffCode(String staffCode) { this.staffCode = staffCode; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setAppUserId(Long appUserId) { this.appUserId = appUserId; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(Role role) { this.role = role; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getId() { return id; }
    public String getStaffCode() { return staffCode; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public Long getAppUserId() { return appUserId; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}