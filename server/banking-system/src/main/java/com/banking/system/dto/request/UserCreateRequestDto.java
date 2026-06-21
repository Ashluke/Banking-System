package com.banking.system.dto.request;

import jakarta.validation.constraints.*;

public class UserCreateRequestDto {
    
    @NotNull
    private Long appUserId;

    @NotBlank
    @Size(max = 50)
    private String firstName;

    @NotBlank
    @Size(max = 50)
    private String lastName;

    @NotBlank
    @Pattern(regexp = "^09\\d{9}$")
    private String phoneNumber;

    @NotBlank
    @Size(max = 255)
    private String address;

    public UserCreateRequestDto() {}

    public UserCreateRequestDto(Long appUserId, String firstName, String lastName, String phoneNumber, String address) {
        this.appUserId = appUserId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.address = address;
    }

    public void setAppUserId(Long appUserId) { this.appUserId = appUserId; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setAddress(String address) { this.address = address; }

    public Long getAppUserId() { return appUserId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getAddress() { return address; }
}
