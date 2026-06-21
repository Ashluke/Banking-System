package com.banking.system.exception;

public class InvalidCredentialsException extends RuntimeException {
    
    public InvalidCredentialsException() {
        super("Invalid Credentials");
    }
}
