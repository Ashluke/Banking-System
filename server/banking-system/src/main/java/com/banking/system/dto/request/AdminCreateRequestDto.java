package com.banking.system.dto.request;

import jakarta.validation.constraints.*;

public class AdminCreateRequestDto {
    
    @NotNull
    private Long appUserId;

    @NotBlank
    @Size(max = 30)
    private String staffCode;

    @NotBlank
    @Size(max = 50)
    private String firstName;

    @NotBlank
    @Size(max = 50)
    private String lastName;

    public AdminCreateRequestDto() {}

    public AdminCreateRequestDto(Long appUserId, String staffCode, String firstName, String lastName) {
        this.appUserId = appUserId;
        this.staffCode = staffCode;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public void setAppUserId(Long appUserId) { this.appUserId = appUserId; }
    public void setStaffCode(String staffCode) { this.staffCode = staffCode; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public Long getAppUserId() { return appUserId; }
    public String getStaffCode() { return staffCode; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }

}
