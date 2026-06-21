package com.banking.system.exception;

public class AccountNotActiveException extends RuntimeException {

    public AccountNotActiveException(Long accountId) {
        super("Account " + accountId + " is not active");
    }
}