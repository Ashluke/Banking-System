package com.banking.system.controller;

import com.banking.system.dto.request.DepositRequestDto;
import com.banking.system.dto.request.TransferRequestDto;
import com.banking.system.dto.request.WithdrawRequestDto;
import com.banking.system.dto.response.TransactionResponseDto;
import com.banking.system.exception.AccountNotActiveException;
import com.banking.system.exception.InsufficientBalanceException;
import com.banking.system.exception.InvalidTransferException;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.exception.UnauthorizedActionException;
import com.banking.system.model.enums.TransactionType;
import com.banking.system.security.JWTService;
import com.banking.system.security.SecurityConfig;
import com.banking.system.services.TransactionService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@Import(SecurityConfig.class)
public class TransactionControllerTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private JWTService jwtService;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }


    private UsernamePasswordAuthenticationToken userAuth(Long appUserId) {
        return new UsernamePasswordAuthenticationToken(
            appUserId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private UsernamePasswordAuthenticationToken adminAuth(Long appUserId) {
        return new UsernamePasswordAuthenticationToken(
            appUserId, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }


    // ===================== DEPOSIT =====================

    @Test
    void deposit_shouldReturn201_whenValidRequest_andUserRole() throws Exception {

        DepositRequestDto request = new DepositRequestDto(1L, BigDecimal.valueOf(100.0));

        TransactionResponseDto response = new TransactionResponseDto(
            1L, 1L, BigDecimal.valueOf(100.0), TransactionType.DEPOSIT, LocalDateTime.now()
        );

        when(transactionService.deposit(any(DepositRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/transactions/deposit")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.bankAccountId").value(1L))
            .andExpect(jsonPath("$.type").value("DEPOSIT"));
    }

    @Test
    void deposit_shouldReturn403_whenAdminRole() throws Exception {

        DepositRequestDto request = new DepositRequestDto(1L, BigDecimal.valueOf(100.0));

        mockMvc.perform(post("/api/transactions/deposit")
                .with(authentication(adminAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());

        verify(transactionService, never()).deposit(any());
    }

    @Test
    void deposit_shouldReturn401_whenUnauthenticated() throws Exception {

        DepositRequestDto request = new DepositRequestDto(1L, BigDecimal.valueOf(100.0));

        mockMvc.perform(post("/api/transactions/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deposit_shouldReturn400_whenAmountMissing() throws Exception {

        DepositRequestDto request = new DepositRequestDto(1L, null);

        mockMvc.perform(post("/api/transactions/deposit")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(transactionService, never()).deposit(any());
    }

    @Test
    void deposit_shouldReturn400_whenAmountNotPositive() throws Exception {

        DepositRequestDto request = new DepositRequestDto(1L, BigDecimal.valueOf(-50.0));

        mockMvc.perform(post("/api/transactions/deposit")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(transactionService, never()).deposit(any());
    }

    @Test
    void deposit_shouldReturn400_whenAccountNotActive() throws Exception {

        DepositRequestDto request = new DepositRequestDto(1L, BigDecimal.valueOf(100.0));

        when(transactionService.deposit(any(DepositRequestDto.class)))
            .thenThrow(new AccountNotActiveException(1L));

        mockMvc.perform(post("/api/transactions/deposit")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void deposit_shouldReturn404_whenAccountNotFound() throws Exception {

        DepositRequestDto request = new DepositRequestDto(1L, BigDecimal.valueOf(100.0));

        when(transactionService.deposit(any(DepositRequestDto.class)))
            .thenThrow(new ResourceNotFoundException("Account not found"));

        mockMvc.perform(post("/api/transactions/deposit")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }


    // ===================== WITHDRAW =====================

    @Test
    void withdraw_shouldReturn201_whenValidRequest() throws Exception {

        WithdrawRequestDto request = new WithdrawRequestDto(1L, BigDecimal.valueOf(50.0));

        TransactionResponseDto response = new TransactionResponseDto(
            1L, 1L, BigDecimal.valueOf(50.0), TransactionType.WITHDRAW, LocalDateTime.now()
        );

        when(transactionService.withdraw(any(WithdrawRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/transactions/withdraw")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("WITHDRAW"));
    }

    @Test
    void withdraw_shouldReturn400_whenInsufficientBalance() throws Exception {

        WithdrawRequestDto request = new WithdrawRequestDto(1L, BigDecimal.valueOf(5000.0));

        when(transactionService.withdraw(any(WithdrawRequestDto.class)))
            .thenThrow(new InsufficientBalanceException("Insufficient balance"));

        mockMvc.perform(post("/api/transactions/withdraw")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Insufficient balance"));
    }

    @Test
    void withdraw_shouldReturn403_whenAdminRole() throws Exception {

        WithdrawRequestDto request = new WithdrawRequestDto(1L, BigDecimal.valueOf(50.0));

        mockMvc.perform(post("/api/transactions/withdraw")
                .with(authentication(adminAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    void withdraw_shouldReturn400_whenAccountIdMissing() throws Exception {

        WithdrawRequestDto request = new WithdrawRequestDto(null, BigDecimal.valueOf(50.0));

        mockMvc.perform(post("/api/transactions/withdraw")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }


    // ===================== TRANSFER =====================

    @Test
    void transfer_shouldReturn201_whenValidRequest() throws Exception {

        TransferRequestDto request = new TransferRequestDto(1L, 2L, BigDecimal.valueOf(100.0));

        TransactionResponseDto response = new TransactionResponseDto(
            1L, 1L, null, BigDecimal.valueOf(100.0), TransactionType.TRANSFER_OUT, LocalDateTime.now()
        );

        when(transactionService.transfer(any(TransferRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/transactions/transfer")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("TRANSFER_OUT"));
    }

    @Test
    void transfer_shouldReturn400_whenSameAccount() throws Exception {

        TransferRequestDto request = new TransferRequestDto(1L, 1L, BigDecimal.valueOf(100.0));

        mockMvc.perform(post("/api/transactions/transfer")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(transactionService, never()).transfer(any());
    }

    @Test
    void transfer_shouldReturn400_whenServiceThrowsInvalidTransfer() throws Exception {

        TransferRequestDto request = new TransferRequestDto(1L, 2L, BigDecimal.valueOf(100.0));

        when(transactionService.transfer(any(TransferRequestDto.class)))
            .thenThrow(new InvalidTransferException("Cannot transfer to the same account"));

        mockMvc.perform(post("/api/transactions/transfer")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void transfer_shouldReturn404_whenDestinationAccountNotFound() throws Exception {

        TransferRequestDto request = new TransferRequestDto(1L, 2L, BigDecimal.valueOf(100.0));

        when(transactionService.transfer(any(TransferRequestDto.class)))
            .thenThrow(new ResourceNotFoundException("Destination account not found"));

        mockMvc.perform(post("/api/transactions/transfer")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void transfer_shouldReturn403_whenAdminRole() throws Exception {

        TransferRequestDto request = new TransferRequestDto(1L, 2L, BigDecimal.valueOf(100.0));

        mockMvc.perform(post("/api/transactions/transfer")
                .with(authentication(adminAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }


    // ===================== GET BY ACCOUNT ID (with filters) =====================

    @Test
    void getByAccountId_shouldReturn200_withNoFilters_whenUserRole() throws Exception {

        TransactionResponseDto tx = new TransactionResponseDto(
            1L, 1L, BigDecimal.valueOf(100.0), TransactionType.DEPOSIT, LocalDateTime.now()
        );
        Page<TransactionResponseDto> page = new PageImpl<>(List.of(tx));

        when(transactionService.getByAccountId(
            anyLong(), anyLong(), anyBoolean(),
            isNull(), isNull(), isNull(), isNull(), isNull(),
            any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(get("/api/transactions/account/1")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].type").value("DEPOSIT"));
    }

    @Test
    void getByAccountId_shouldReturn200_withTypeFilter() throws Exception {

        TransactionResponseDto tx = new TransactionResponseDto(
            1L, 1L, BigDecimal.valueOf(100.0), TransactionType.WITHDRAW, LocalDateTime.now()
        );
        Page<TransactionResponseDto> page = new PageImpl<>(List.of(tx));

        when(transactionService.getByAccountId(
            anyLong(), anyLong(), anyBoolean(),
            eq(TransactionType.WITHDRAW), isNull(), isNull(), isNull(), isNull(),
            any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(get("/api/transactions/account/1")
                .param("type", "WITHDRAW")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].type").value("WITHDRAW"));
    }

    @Test
    void getByAccountId_shouldReturn200_withDateRangeFilter() throws Exception {

        TransactionResponseDto tx = new TransactionResponseDto(
            1L, 1L, BigDecimal.valueOf(100.0), TransactionType.DEPOSIT, LocalDateTime.now()
        );
        Page<TransactionResponseDto> page = new PageImpl<>(List.of(tx));

        when(transactionService.getByAccountId(
            anyLong(), anyLong(), anyBoolean(),
            isNull(), any(LocalDateTime.class), any(LocalDateTime.class), isNull(), isNull(),
            any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(get("/api/transactions/account/1")
                .param("from", "2025-01-01T00:00:00")
                .param("to", "2025-12-31T23:59:59")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isOk());
    }

    @Test
    void getByAccountId_shouldReturn200_withAmountRangeFilter() throws Exception {

        TransactionResponseDto tx = new TransactionResponseDto(
            1L, 1L, BigDecimal.valueOf(500.0), TransactionType.DEPOSIT, LocalDateTime.now()
        );
        Page<TransactionResponseDto> page = new PageImpl<>(List.of(tx));

        when(transactionService.getByAccountId(
            anyLong(), anyLong(), anyBoolean(),
            isNull(), isNull(), isNull(), any(BigDecimal.class), any(BigDecimal.class),
            any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(get("/api/transactions/account/1")
                .param("minAmount", "100.00")
                .param("maxAmount", "1000.00")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isOk());
    }

    @Test
    void getByAccountId_shouldReturn200_whenAdminRole() throws Exception {

        TransactionResponseDto tx = new TransactionResponseDto(
            1L, 1L, BigDecimal.valueOf(100.0), TransactionType.DEPOSIT, LocalDateTime.now()
        );
        Page<TransactionResponseDto> page = new PageImpl<>(List.of(tx));

        when(transactionService.getByAccountId(
            anyLong(), anyLong(), anyBoolean(),
            isNull(), isNull(), isNull(), isNull(), isNull(),
            any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(get("/api/transactions/account/1")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isOk());
    }

    @Test
    void getByAccountId_shouldReturn403_whenNotOwner() throws Exception {

        when(transactionService.getByAccountId(
            anyLong(), anyLong(), anyBoolean(),
            isNull(), isNull(), isNull(), isNull(), isNull(),
            any(Pageable.class)
        )).thenThrow(new UnauthorizedActionException("You do not own this account"));

        mockMvc.perform(get("/api/transactions/account/1")
                .with(authentication(userAuth(999L))))
            .andExpect(status().isForbidden());
    }

    @Test
    void getByAccountId_shouldReturn404_whenAccountNotFound() throws Exception {

        when(transactionService.getByAccountId(
            anyLong(), anyLong(), anyBoolean(),
            isNull(), isNull(), isNull(), isNull(), isNull(),
            any(Pageable.class)
        )).thenThrow(new ResourceNotFoundException("Account not found"));

        mockMvc.perform(get("/api/transactions/account/1")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isNotFound());
    }

    @Test
    void getByAccountId_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(get("/api/transactions/account/1"))
            .andExpect(status().isUnauthorized());
    }
}