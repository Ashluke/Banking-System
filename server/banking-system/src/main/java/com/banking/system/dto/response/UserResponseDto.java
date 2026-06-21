package com.banking.system.dto.response;

public class UserResponseDto {
    
    private Long id;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;
    private Long appUserId;

    public UserResponseDto() {}

    public UserResponseDto(Long id, String firstName, String lastName, String phoneNumber, String address, Long appUserId) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.appUserId = appUserId;
    }

    public void setId(Long id) { this.id = id; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setAddress(String address) { this.address = address; }
    public void setAppUserId(Long appUserId) { this.appUserId = appUserId; }

    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getAddress() { return address; }
    public Long getAppUserId() { return appUserId; }
    
}
