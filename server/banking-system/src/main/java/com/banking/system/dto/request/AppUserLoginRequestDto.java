package com.banking.system.dto.request;

import jakarta.validation.constraints.*;

public class AppUserLoginRequestDto {
    
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    public AppUserLoginRequestDto() {}

    public AppUserLoginRequestDto(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
}
