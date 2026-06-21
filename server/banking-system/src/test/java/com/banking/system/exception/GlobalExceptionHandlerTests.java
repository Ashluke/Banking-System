package com.banking.system.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();


    @Test
    void handleResourceNotFound_shouldReturn404() {

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(
            new ResourceNotFoundException("Account not found")
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Not Found", response.getBody().getError());
        assertEquals("Account not found", response.getBody().getMessage());
    }

    @Test
    void handleDuplicateResource_shouldReturn409() {

        ResponseEntity<ErrorResponse> response = handler.handleDuplicateResource(
            new DuplicateResourceException("Email already exists")
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Conflict", response.getBody().getError());
        assertEquals("Email already exists", response.getBody().getMessage());
    }

    @Test
    void handleInsufficientBalance_shouldReturn400() {

        ResponseEntity<ErrorResponse> response = handler.handleInsufficientBalance(
            new InsufficientBalanceException("Insufficient balance")
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("Insufficient balance", response.getBody().getMessage());
    }

    @Test
    void handleUnauthorizedAction_shouldReturn403() {

        ResponseEntity<ErrorResponse> response = handler.handleUnauthorizedAction(
            new UnauthorizedActionException("You do not own this account")
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Forbidden", response.getBody().getError());
        assertEquals("You do not own this account", response.getBody().getMessage());
    }

    @Test
    void handleAccountLimit_shouldReturn400() {

        ResponseEntity<ErrorResponse> response = handler.handleAccountLimit(
            new AccountLimitExceedException("Maximum of 3 bank accounts is allowed per user")
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Bad Request", response.getBody().getError());
    }

    @Test
    void handleAccountNotActive_shouldReturn400() {

        ResponseEntity<ErrorResponse> response = handler.handleAccountNotActive(
            new AccountNotActiveException(1L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("Account 1 is not active", response.getBody().getMessage());
    }

    @Test
    void handleInvalidTransfer_shouldReturn400() {

        ResponseEntity<ErrorResponse> response = handler.handleInvalidTransfer(
            new InvalidTransferException("Cannot transfer to the same account")
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Bad Request", response.getBody().getError());
    }

    @Test
    void handleInvalidCredentials_shouldReturn401() {

        ResponseEntity<ErrorResponse> response = handler.handleInvalidCredentials(
            new InvalidCredentialsException()
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Unauthorized", response.getBody().getError());
    }

    @Test
    void handleInvalidAccountState_shouldReturn400() {

        ResponseEntity<ErrorResponse> response = handler.handleInvalidAccountState(
            new InvalidAccountStateException("Closed accounts cannot be frozen")
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("Closed accounts cannot be frozen", response.getBody().getMessage());
    }

    @Test
    void handleGeneralException_shouldReturn500_withGenericMessage() {

        ResponseEntity<ErrorResponse> response = handler.handleGeneralException(
            new RuntimeException("some internal detail that shouldn't leak")
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Internal Server Error", response.getBody().getError());
        assertEquals("Something went wrong. Please try again later.", response.getBody().getMessage());
    }

    @Test
    void handleAccessDenied_shouldReturn403_withGenericMessage() {

        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(
            new AccessDeniedException("Access is denied")
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Forbidden", response.getBody().getError());
        assertEquals("You do not have permission to access this resource", response.getBody().getMessage());
    }

    @Test
    void handleValidation_shouldReturn400_withSingleFieldError() {

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(
            List.of(new FieldError("request", "email", "Invalid email format"))
        );

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation error", response.getBody().getError());
        assertEquals("email: Invalid email format", response.getBody().getMessage());
    }

    @Test
    void handleValidation_shouldCombineMultipleFieldErrors() {

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(
            List.of(
                new FieldError("request", "email", "Invalid email format"),
                new FieldError("request", "password", "Password is required")
            )
        );

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("email: Invalid email format"));
        assertTrue(response.getBody().getMessage().contains("password: Password is required"));
    }

    @Test
    void handleValidation_shouldReturnDefaultMessage_whenNoFieldErrors() {

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation error", response.getBody().getMessage());
    }
}