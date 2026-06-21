package com.banking.system.dto.request;

import jakarta.validation.constraints.*;

public class BankAccountCreateRequestDto {
    
    @NotNull
    private Long userId;

    public BankAccountCreateRequestDto() {}

    public void setUserId(Long userId) { this.userId = userId; }

    public Long getUserId() { return userId; }

}
