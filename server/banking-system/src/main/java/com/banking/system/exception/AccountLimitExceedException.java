package com.banking.system.exception;

public class AccountLimitExceedException extends RuntimeException {
    
    public AccountLimitExceedException(String message) {
        super(message);
    }
}
