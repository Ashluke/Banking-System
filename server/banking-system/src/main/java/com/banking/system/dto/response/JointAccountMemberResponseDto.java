package com.banking.system.dto.response;

import com.banking.system.model.enums.JointAccountRole;

import java.time.LocalDateTime;

public class JointAccountMemberResponseDto {

    private Long id;
    private Long bankAccountId;
    private Long userId;
    private String firstName;
    private String lastName;
    private JointAccountRole role;
    private LocalDateTime joinedAt;

    public JointAccountMemberResponseDto() {}

    public JointAccountMemberResponseDto(Long id, Long bankAccountId, Long userId,
            String firstName, String lastName, JointAccountRole role, LocalDateTime joinedAt) {
        this.id = id;
        this.bankAccountId = bankAccountId;
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public void setId(Long id) { this.id = id; }
    public void setBankAccountId(Long bankAccountId) { this.bankAccountId = bankAccountId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setRole(JointAccountRole role) { this.role = role; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }

    public Long getId() { return id; }
    public Long getBankAccountId() { return bankAccountId; }
    public Long getUserId() { return userId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public JointAccountRole getRole() { return role; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
}