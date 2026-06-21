package com.banking.system.controller;

import com.banking.system.dto.request.BankAccountCreateRequestDto;
import com.banking.system.dto.response.BankAccountResponseDto;
import com.banking.system.exception.AccountLimitExceedException;
import com.banking.system.exception.InvalidAccountStateException;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.exception.UnauthorizedActionException;
import com.banking.system.model.enums.AccountStatus;
import com.banking.system.security.JWTService;
import com.banking.system.security.SecurityConfig;
import com.banking.system.services.BankAccountService;

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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BankAccountController.class)
@Import(SecurityConfig.class)
public class BankAccountControllerTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private BankAccountService bankAccountService;

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
            appUserId,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private UsernamePasswordAuthenticationToken adminAuth(Long appUserId) {
        return new UsernamePasswordAuthenticationToken(
            appUserId,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }


    // ===================== CREATE =====================

    @Test
    void create_shouldReturn201_whenAdmin() throws Exception {

        BankAccountCreateRequestDto request = new BankAccountCreateRequestDto();
        request.setUserId(1L);

        BankAccountResponseDto response = new BankAccountResponseDto(
            1L, BigDecimal.ZERO, AccountStatus.ACTIVE, 1L
        );

        when(bankAccountService.createAccount(any(BankAccountCreateRequestDto.class), eq(99L)))
            .thenReturn(response);

        mockMvc.perform(post("/api/accounts")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void create_shouldReturn403_whenUserRole() throws Exception {

        BankAccountCreateRequestDto request = new BankAccountCreateRequestDto();
        request.setUserId(1L);

        mockMvc.perform(post("/api/accounts")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());

        verify(bankAccountService, never()).createAccount(any(), any());
    }

    @Test
    void create_shouldReturn401_whenUnauthenticated() throws Exception {

        BankAccountCreateRequestDto request = new BankAccountCreateRequestDto();
        request.setUserId(1L);

        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void create_shouldReturn404_whenUserNotFound() throws Exception {

        BankAccountCreateRequestDto request = new BankAccountCreateRequestDto();
        request.setUserId(1L);

        when(bankAccountService.createAccount(any(BankAccountCreateRequestDto.class), eq(99L)))
            .thenThrow(new ResourceNotFoundException("AppUser not found"));

        mockMvc.perform(post("/api/accounts")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void create_shouldReturn400_whenAccountLimitExceeded() throws Exception {

        BankAccountCreateRequestDto request = new BankAccountCreateRequestDto();
        request.setUserId(1L);

        when(bankAccountService.createAccount(any(BankAccountCreateRequestDto.class), eq(99L)))
            .thenThrow(new AccountLimitExceedException("Maximum of 3 bank accounts is allowed per user"));

        mockMvc.perform(post("/api/accounts")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400_whenUserIdMissing() throws Exception {

        BankAccountCreateRequestDto request = new BankAccountCreateRequestDto();

        mockMvc.perform(post("/api/accounts")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(bankAccountService, never()).createAccount(any(), any());
    }


    // ===================== GET BY ID =====================

    @Test
    void getById_shouldReturn200_whenOwner() throws Exception {

        BankAccountResponseDto response = new BankAccountResponseDto(
            1L, BigDecimal.valueOf(500.0), AccountStatus.ACTIVE, 1L
        );

        when(bankAccountService.getBankAccountById(eq(1L), eq(5L), eq(false)))
            .thenReturn(response);

        mockMvc.perform(get("/api/accounts/1")
                .with(authentication(userAuth(5L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getById_shouldReturn200_whenAdmin() throws Exception {

        BankAccountResponseDto response = new BankAccountResponseDto(
            1L, BigDecimal.valueOf(500.0), AccountStatus.ACTIVE, 1L
        );

        when(bankAccountService.getBankAccountById(eq(1L), eq(99L), eq(true)))
            .thenReturn(response);

        mockMvc.perform(get("/api/accounts/1")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isOk());
    }

    @Test
    void getById_shouldReturn403_whenNotOwner() throws Exception {

        when(bankAccountService.getBankAccountById(eq(1L), eq(999L), eq(false)))
            .thenThrow(new UnauthorizedActionException("You do not own this account"));

        mockMvc.perform(get("/api/accounts/1")
                .with(authentication(userAuth(999L))))
            .andExpect(status().isForbidden());
    }

    @Test
    void getById_shouldReturn404_whenNotFound() throws Exception {

        when(bankAccountService.getBankAccountById(eq(1L), eq(5L), eq(false)))
            .thenThrow(new ResourceNotFoundException("Account not found"));

        mockMvc.perform(get("/api/accounts/1")
                .with(authentication(userAuth(5L))))
            .andExpect(status().isNotFound());
    }

    @Test
    void getById_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(get("/api/accounts/1"))
            .andExpect(status().isUnauthorized());
    }


    // ===================== GET BY USER ID =====================

    @Test
    void getByUserId_shouldReturn200_whenUserRole() throws Exception {

        BankAccountResponseDto response = new BankAccountResponseDto(
            1L, BigDecimal.valueOf(500.0), AccountStatus.ACTIVE, 1L
        );

        Page<BankAccountResponseDto> page = new PageImpl<>(List.of(response));

        when(bankAccountService.getByUserId(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/accounts/users/1")
                .with(authentication(userAuth(5L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    }

    @Test
    void getByUserId_shouldReturn200_whenAdmin() throws Exception {

        BankAccountResponseDto response = new BankAccountResponseDto(
            1L, BigDecimal.valueOf(500.0), AccountStatus.ACTIVE, 1L
        );

        Page<BankAccountResponseDto> page = new PageImpl<>(List.of(response));

        when(bankAccountService.getByUserId(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/accounts/users/1")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isOk());
    }

    @Test
    void getByUserId_shouldReturn404_whenUserNotFound() throws Exception {

        when(bankAccountService.getByUserId(eq(1L), any(Pageable.class)))
            .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/accounts/users/1")
                .with(authentication(userAuth(5L))))
            .andExpect(status().isNotFound());
    }

    @Test
    void getByUserId_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(get("/api/accounts/users/1"))
            .andExpect(status().isUnauthorized());
    }


    // ===================== CLOSE ACCOUNT =====================

    @Test
    void closeAccount_shouldReturn200_whenAdmin() throws Exception {

        BankAccountResponseDto response = new BankAccountResponseDto(
            1L, BigDecimal.valueOf(500.0), AccountStatus.CLOSED, 1L
        );

        when(bankAccountService.closeAccount(eq(1L), eq(99L), eq(true)))
            .thenReturn(response);

        mockMvc.perform(put("/api/accounts/1/close")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void closeAccount_shouldReturn403_whenUserRole() throws Exception {

        mockMvc.perform(put("/api/accounts/1/close")
                .with(authentication(userAuth(5L))))
            .andExpect(status().isForbidden());

        verify(bankAccountService, never()).closeAccount(any(), any(), anyBoolean());
    }

    @Test
    void closeAccount_shouldReturn404_whenNotFound() throws Exception {

        when(bankAccountService.closeAccount(eq(1L), eq(99L), eq(true)))
            .thenThrow(new ResourceNotFoundException("Account not found"));

        mockMvc.perform(put("/api/accounts/1/close")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isNotFound());
    }

    @Test
    void closeAccount_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(put("/api/accounts/1/close"))
            .andExpect(status().isUnauthorized());
    }


    // ===================== FREEZE ACCOUNT =====================

    @Test
    void freezeAccount_shouldReturn200_whenAdmin() throws Exception {

        BankAccountResponseDto response = new BankAccountResponseDto(
            1L, BigDecimal.valueOf(500.0), AccountStatus.FROZEN, 1L
        );

        when(bankAccountService.freezeAccount(eq(1L), eq(99L), eq(true)))
            .thenReturn(response);

        mockMvc.perform(put("/api/accounts/1/freeze")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("FROZEN"));
    }

    @Test
    void freezeAccount_shouldReturn403_whenUserRole() throws Exception {

        mockMvc.perform(put("/api/accounts/1/freeze")
                .with(authentication(userAuth(5L))))
            .andExpect(status().isForbidden());

        verify(bankAccountService, never()).freezeAccount(any(), any(), anyBoolean());
    }

    @Test
    void freezeAccount_shouldReturn400_whenAccountClosed() throws Exception {

        when(bankAccountService.freezeAccount(eq(1L), eq(99L), eq(true)))
            .thenThrow(new InvalidAccountStateException("Closed accounts cannot be frozen"));

        mockMvc.perform(put("/api/accounts/1/freeze")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void freezeAccount_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(put("/api/accounts/1/freeze"))
            .andExpect(status().isUnauthorized());
    }


    // ===================== UNFREEZE ACCOUNT =====================

    @Test
    void unfreezeAccount_shouldReturn200_whenAdmin() throws Exception {

        BankAccountResponseDto response = new BankAccountResponseDto(
            1L, BigDecimal.valueOf(500.0), AccountStatus.ACTIVE, 1L
        );

        when(bankAccountService.unfreezeAccount(eq(1L), eq(99L), eq(true)))
            .thenReturn(response);

        mockMvc.perform(put("/api/accounts/1/unfreeze")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void unfreezeAccount_shouldReturn403_whenUserRole() throws Exception {

        mockMvc.perform(put("/api/accounts/1/unfreeze")
                .with(authentication(userAuth(5L))))
            .andExpect(status().isForbidden());

        verify(bankAccountService, never()).unfreezeAccount(any(), any(), anyBoolean());
    }

    @Test
    void unfreezeAccount_shouldReturn400_whenAccountClosed() throws Exception {

        when(bankAccountService.unfreezeAccount(eq(1L), eq(99L), eq(true)))
            .thenThrow(new InvalidAccountStateException("Closed accounts cannot be unfrozen"));

        mockMvc.perform(put("/api/accounts/1/unfreeze")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void unfreezeAccount_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(put("/api/accounts/1/unfreeze"))
            .andExpect(status().isUnauthorized());
    }
}