package com.banking.system.dto.request;

import jakarta.validation.constraints.*;

public class UserUpdateRequestDto {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^09\\d{9}$", message = "Phone number must be a valid Philippine mobile number (e.g. 09171234567)")
    private String phoneNumber;

    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    public UserUpdateRequestDto() {}

    public UserUpdateRequestDto(String phoneNumber, String address) {
        this.phoneNumber = phoneNumber;
        this.address = address;
    }

    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setAddress(String address) { this.address = address; }

    public String getPhoneNumber() { return phoneNumber; }
    public String getAddress() { return address; }
}