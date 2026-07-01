package com.banking.system.dto.response;

import java.time.LocalDateTime;

import com.banking.system.model.enums.Role;

public class UserRegisterResponseDto {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;
    private Long appUserId;
    private Role role;
    private LocalDateTime createdAt;

    public UserRegisterResponseDto() {}

    public UserRegisterResponseDto(Long id, String email, String firstName, String lastName,
            String phoneNumber, String address, Long appUserId, Role role, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.appUserId = appUserId;
        this.role = role;
        this.createdAt = createdAt;
    }

    public void setId(Long id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setAddress(String address) { this.address = address; }
    public void setAppUserId(Long appUserId) { this.appUserId = appUserId; }
    public void setRole(Role role) { this.role = role; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getAddress() { return address; }
    public Long getAppUserId() { return appUserId; }
    public Role getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}