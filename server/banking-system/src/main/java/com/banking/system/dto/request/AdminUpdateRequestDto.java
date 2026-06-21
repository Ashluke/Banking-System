package com.banking.system.dto.request;

import jakarta.validation.constraints.*;

public class AdminUpdateRequestDto {
    
    @NotBlank
    @Size(max = 30)
    private String staffCode;

    @NotBlank
    @Size(max = 50)
    private String firstName;

    @NotBlank
    @Size(max = 50)
    private String lastName;

    public AdminUpdateRequestDto() {}

    public AdminUpdateRequestDto(String staffCode, String firstName, String lastName) { 
        this.staffCode = staffCode;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public void setStaffCode(String staffCode) { this.staffCode = staffCode; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getStaffCode() { return staffCode; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    
}
