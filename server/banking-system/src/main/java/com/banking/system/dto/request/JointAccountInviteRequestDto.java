package com.banking.system.dto.request;

import jakarta.validation.constraints.NotNull;

public class JointAccountInviteRequestDto {

    @NotNull(message = "Co-owner user ID is required")
    private Long coOwnerUserId;

    public JointAccountInviteRequestDto() {}

    public JointAccountInviteRequestDto(Long coOwnerUserId) {
        this.coOwnerUserId = coOwnerUserId;
    }

    public void setCoOwnerUserId(Long coOwnerUserId) { this.coOwnerUserId = coOwnerUserId; }

    public Long getCoOwnerUserId() { return coOwnerUserId; }
}