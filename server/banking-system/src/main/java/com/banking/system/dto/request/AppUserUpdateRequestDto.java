package com.banking.system.dto.request;

import jakarta.validation.constraints.*;

public class AppUserUpdateRequestDto {
    
    @NotBlank
    @Email
    private String email;

     @NotBlank
    @Size(min = 8, max = 20)
    private String password;

    public AppUserUpdateRequestDto() {}

    public AppUserUpdateRequestDto(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    
}
