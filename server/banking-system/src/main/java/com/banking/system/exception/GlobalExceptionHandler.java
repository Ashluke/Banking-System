package com.banking.system.exception;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String error,
            String message
    ) {
        return new ResponseEntity<>(
                new ErrorResponse(
                        LocalDateTime.now(),
                        status.value(),
                        error,
                        message
                ),
                status
        );
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildResponse(
            HttpStatus.NOT_FOUND, 
            "Not Found", 
            ex.getMessage()
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex) {
        return buildResponse(
            HttpStatus.CONFLICT,
            "Conflict",
            ex.getMessage()
        );
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException ex) {
        return buildResponse(
            HttpStatus.BAD_REQUEST, 
            "Bad Request", 
            ex.getMessage()
        );
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAction(UnauthorizedActionException ex) {
        return buildResponse(
            HttpStatus.FORBIDDEN, 
            "Forbidden", 
            ex.getMessage()
        );
    }


    @ExceptionHandler(AccountLimitExceedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLimit(AccountLimitExceedException ex) {
        return buildResponse(
            HttpStatus.BAD_REQUEST, 
            "Bad Request", 
            ex.getMessage()
        );
    }

    @ExceptionHandler(AccountNotActiveException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotActive(AccountNotActiveException ex) {
        return buildResponse(
            HttpStatus.BAD_REQUEST, 
            "Bad Request", 
            ex.getMessage()
        );
    }

    @ExceptionHandler(InvalidTransferException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransfer(InvalidTransferException ex) {
        return buildResponse(
            HttpStatus.BAD_REQUEST, 
            "Bad Request", 
            ex.getMessage()
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(Exception ex) {
        return buildResponse(
            HttpStatus.UNAUTHORIZED, 
            "Unauthorized", 
            ex.getMessage()
        );
    }

    @ExceptionHandler(InvalidAccountStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAccountState(InvalidAccountStateException ex) {
        return buildResponse(
            HttpStatus.BAD_REQUEST, 
            "Bad Request", 
            ex.getMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        return buildResponse(
            HttpStatus.INTERNAL_SERVER_ERROR, 
            "Internal Server Error", 
            "Something went wrong. Please try again later."
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse(
            HttpStatus.FORBIDDEN, 
            "Forbidden", 
            "You do not have permission to access this resource"
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .reduce((a, b) -> a + ", " + b)
            .orElse("Validation error");

        return buildResponse(
            HttpStatus.BAD_REQUEST, 
            "Validation error", 
            message
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParams(MissingServletRequestParameterException ex) {
        return buildResponse(
            HttpStatus.BAD_REQUEST, 
            "Bad request", 
            ex.getMessage()
        );
    }
}
