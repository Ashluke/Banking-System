package com.banking.system.dto.response;

import com.banking.system.model.enums.Role;

import java.time.LocalDateTime;

public class AppUserResponseDto {
    
    private Long id;
    private String email;
    private Role role;
    private LocalDateTime createdAt;

    public AppUserResponseDto() {}

    public AppUserResponseDto(Long id, String email, Role role, LocalDateTime createdAt) { 
        this.id = id;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
    }

    public void setId(Long id) { this.id = id; }
    public void setEmail(String email) { this.email = email; } 
    public void setRole(Role role) { this.role = role; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }

}
