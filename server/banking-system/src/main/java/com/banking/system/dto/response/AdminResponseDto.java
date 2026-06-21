package com.banking.system.dto.response;

public class AdminResponseDto {
    
    private Long id;
    private String staffCode;
    private String firstName;
    private String lastName;
    private Long appUserId;

    public AdminResponseDto() {}

    public AdminResponseDto(Long id, String staffCode, String firstName, String lastName, Long appUserId) {
        this.id = id;
        this.staffCode = staffCode;
        this.firstName = firstName;
        this.lastName = lastName;
        this.appUserId = appUserId;
    }

    public void setId(Long id) { this.id = id; }
    public void setStaffCode(String staffCode) { this.staffCode = staffCode; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setAppUserId(Long appUserId) { this.appUserId = appUserId; }

    public Long getId() { return id; }
    public String getStaffCode() { return staffCode; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public Long getAppUserId() { return appUserId; }

}
