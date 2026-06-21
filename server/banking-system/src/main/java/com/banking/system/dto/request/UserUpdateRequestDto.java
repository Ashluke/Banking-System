package com.banking.system.dto.request;

import jakarta.validation.constraints.*;

public class UserUpdateRequestDto {
    
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

    public UserUpdateRequestDto() {}

    public UserUpdateRequestDto(String firstName, String lastName, String phoneNumber, String address) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.address = address;
    }

    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setAddress(String address) { this.address = address; }

    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getAddress() { return address; }
}
