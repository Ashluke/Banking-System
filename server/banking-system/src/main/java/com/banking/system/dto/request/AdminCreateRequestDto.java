package com.banking.system.dto.request;

import jakarta.validation.constraints.*;

public class AdminCreateRequestDto {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters")
    private String password;

    @NotBlank(message = "Staff code is required")
    @Size(max = 30, message = "Staff code must not exceed 30 characters")
    private String staffCode;

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    public AdminCreateRequestDto() {}

    public AdminCreateRequestDto(String email, String password, String staffCode, String firstName, String lastName) {
        this.email = email;
        this.password = password;
        this.staffCode = staffCode;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setStaffCode(String staffCode) { this.staffCode = staffCode; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getStaffCode() { return staffCode; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
}