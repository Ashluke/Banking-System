package com.banking.system.exception;

public class InvalidAccountStateException extends RuntimeException {
    
    public InvalidAccountStateException(String message) {
        super(message);
    }
}
