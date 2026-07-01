package com.banking.system.exception;

public class MissingServletRequestParameterException extends RuntimeException {

    public MissingServletRequestParameterException(String message) {
        super(message);
    }
}
