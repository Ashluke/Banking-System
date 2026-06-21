package com.banking.system.dto.request;

import com.banking.system.model.enums.Role;

import jakarta.validation.constraints.*;

public class AppUserCreateRequestDto {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(
        min = 8,
        max = 20,
        message = "Password must be between 8 and 20 characters"
    )
    private String password;

    @NotNull(message = "Role is required")
    private Role role;

    public AppUserCreateRequestDto() {}

    public AppUserCreateRequestDto(String email, String password, Role role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setRole(Role role) { this.role = role; }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }

}
