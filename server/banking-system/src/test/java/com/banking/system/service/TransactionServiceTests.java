package com.banking.system.service;

import com.banking.system.dto.request.DepositRequestDto;
import com.banking.system.dto.request.TransferRequestDto;
import com.banking.system.dto.request.WithdrawRequestDto;
import com.banking.system.dto.response.TransactionResponseDto;
import com.banking.system.exception.AccountNotActiveException;
import com.banking.system.exception.InsufficientBalanceException;
import com.banking.system.exception.InvalidTransferException;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.exception.UnauthorizedActionException;
import com.banking.system.model.entities.AppUser;
import com.banking.system.model.entities.BankAccount;
import com.banking.system.model.entities.Transaction;
import com.banking.system.model.entities.User;
import com.banking.system.model.enums.TransactionType;
import com.banking.system.repository.BankAccountRepository;
import com.banking.system.repository.TransactionRepository;
import com.banking.system.services.BankAccountService;
import com.banking.system.services.TransactionService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTests {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private BankAccountService bankAccountService;

    @InjectMocks
    private TransactionService transactionService;


    // ===================== DEPOSIT =====================

    @Test
    void deposit_shouldIncreaseBalance_andSaveTransaction() {

        BankAccount account = new BankAccount();
        account.setBalance(BigDecimal.valueOf(500.0));

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        doNothing().when(bankAccountService).validateActive(any());
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        DepositRequestDto request = new DepositRequestDto();
        request.setAccountId(1L);
        request.setAmount(BigDecimal.valueOf(200.0));

        transactionService.deposit(request);

        assertEquals(0, BigDecimal.valueOf(700.0).compareTo(account.getBalance()));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void deposit_shouldThrowException_whenAccountNotActive() {

        BankAccount account = new BankAccount();
        account.setBalance(BigDecimal.valueOf(500.0));

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        doThrow(new AccountNotActiveException(account.getId()))
            .when(bankAccountService).validateActive(any());

        DepositRequestDto request = new DepositRequestDto();
        request.setAccountId(1L);
        request.setAmount(BigDecimal.valueOf(200.0));

        assertThrows(AccountNotActiveException.class, () -> transactionService.deposit(request));

        verify(bankAccountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void deposit_shouldThrowException_whenAccountNotFound() {

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.empty());

        DepositRequestDto request = new DepositRequestDto();
        request.setAccountId(1L);
        request.setAmount(BigDecimal.valueOf(200.0));

        assertThrows(ResourceNotFoundException.class, () -> transactionService.deposit(request));
    }


    // ===================== WITHDRAW =====================

    @Test
    void withdraw_shouldDecreaseBalance_andSaveTransaction() {

        BankAccount account = new BankAccount();
        account.setBalance(BigDecimal.valueOf(1000.0));

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        doNothing().when(bankAccountService).validateActive(any());
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        WithdrawRequestDto request = new WithdrawRequestDto();
        request.setAccountId(1L);
        request.setAmount(BigDecimal.valueOf(300.0));

        transactionService.withdraw(request);

        assertEquals(0, BigDecimal.valueOf(700.0).compareTo(account.getBalance()));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void withdraw_shouldThrowException_whenInsufficientBalance() {

        BankAccount account = new BankAccount();
        account.setBalance(BigDecimal.valueOf(100.0));

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        doNothing().when(bankAccountService).validateActive(any());

        WithdrawRequestDto request = new WithdrawRequestDto();
        request.setAccountId(1L);
        request.setAmount(BigDecimal.valueOf(500.0));

        assertThrows(InsufficientBalanceException.class, () -> transactionService.withdraw(request));
    }

    @Test
    void withdraw_shouldThrowException_whenAccountNotFound() {

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.empty());

        WithdrawRequestDto request = new WithdrawRequestDto();
        request.setAccountId(1L);
        request.setAmount(BigDecimal.valueOf(200.0));

        assertThrows(ResourceNotFoundException.class, () -> transactionService.withdraw(request));
    }


    // ===================== TRANSFER =====================

    @Test
    void transfer_shouldMoveBalance_betweenAccounts_andSaveTransactions() {

        BankAccount from = new BankAccount();
        from.setBalance(BigDecimal.valueOf(1000.0));

        BankAccount to = new BankAccount();
        to.setBalance(BigDecimal.valueOf(500.0));

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(from));
        when(bankAccountRepository.findById(2L)).thenReturn(Optional.of(to));
        doNothing().when(bankAccountService).validateActive(any());
        when(bankAccountRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        TransferRequestDto request = new TransferRequestDto();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(BigDecimal.valueOf(300.0));

        transactionService.transfer(request);

        assertEquals(0, BigDecimal.valueOf(700.0).compareTo(from.getBalance()));
        assertEquals(0, BigDecimal.valueOf(800.0).compareTo(to.getBalance()));
        verify(transactionRepository, atLeast(2)).saveAll(anyList());
    }

    @Test
    void transfer_shouldThrowException_whenInsufficientBalance() {

        BankAccount from = new BankAccount();
        from.setBalance(BigDecimal.valueOf(100.0));

        BankAccount to = new BankAccount();
        to.setBalance(BigDecimal.valueOf(500.0));

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(from));
        when(bankAccountRepository.findById(2L)).thenReturn(Optional.of(to));
        doNothing().when(bankAccountService).validateActive(any());

        TransferRequestDto request = new TransferRequestDto();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(BigDecimal.valueOf(300.0));

        assertThrows(InsufficientBalanceException.class, () -> transactionService.transfer(request));

        verify(transactionRepository, never()).saveAll(any());
    }

    @Test
    void transfer_shouldThrowException_whenSameAccount() {

        TransferRequestDto request = new TransferRequestDto();
        request.setFromAccountId(1L);
        request.setToAccountId(1L);
        request.setAmount(BigDecimal.valueOf(200.0));

        assertThrows(InvalidTransferException.class, () -> transactionService.transfer(request));

        verify(bankAccountRepository, never()).findById(any());
        verify(transactionRepository, never()).saveAll(any());
    }

    @Test
    void transfer_shouldThrowException_whenSourceAccountNotFound() {

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.empty());

        TransferRequestDto request = new TransferRequestDto();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(BigDecimal.valueOf(200.0));

        assertThrows(ResourceNotFoundException.class, () -> transactionService.transfer(request));

        verify(transactionRepository, never()).saveAll(any());
    }

    @Test
    void transfer_shouldThrowException_whenDestinationAccountNotFound() {

        BankAccount from = new BankAccount();
        from.setBalance(BigDecimal.valueOf(1000.0));

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(from));
        when(bankAccountRepository.findById(2L)).thenReturn(Optional.empty());

        TransferRequestDto request = new TransferRequestDto();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(BigDecimal.valueOf(200.0));

        assertThrows(ResourceNotFoundException.class, () -> transactionService.transfer(request));

        verify(transactionRepository, never()).saveAll(any());
    }

    @Test
    void transfer_shouldThrowException_whenAccountNotActive() {

        BankAccount from = new BankAccount();
        from.setBalance(BigDecimal.valueOf(500.0));

        BankAccount to = new BankAccount();
        to.setBalance(BigDecimal.valueOf(500.0));

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(from));
        when(bankAccountRepository.findById(2L)).thenReturn(Optional.of(to));
        doThrow(new AccountNotActiveException(from.getId()))
            .when(bankAccountService).validateActive(any());

        TransferRequestDto request = new TransferRequestDto();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(BigDecimal.valueOf(200.0));

        assertThrows(AccountNotActiveException.class, () -> transactionService.transfer(request));

        verify(bankAccountRepository, never()).saveAll(any());
        verify(transactionRepository, never()).saveAll(any());
    }


    // ===================== GET BY ACCOUNT ID (with filters) =====================

    @SuppressWarnings("unchecked")
    @Test
    void getByAccountId_shouldReturnTransactions_whenAdmin() {

        BankAccount account = new BankAccount();
        account.setBalance(BigDecimal.valueOf(1000.0));

        Transaction tx = new Transaction(account, BigDecimal.valueOf(200.0), TransactionType.DEPOSIT);

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(tx)));

        Page<TransactionResponseDto> result = transactionService.getByAccountId(
            1L, 99L, true, null, null, null, null, null, Pageable.unpaged()
        );

        assertEquals(1, result.getTotalElements());
        verify(transactionRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void getByAccountId_shouldReturnTransactions_whenOwner() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(1L);

        User user = new User();
        user.setAppUser(appUser);

        BankAccount account = new BankAccount();
        account.setBalance(BigDecimal.valueOf(1000.0));
        account.setUser(user);

        Transaction tx = new Transaction(account, BigDecimal.valueOf(200.0), TransactionType.DEPOSIT);

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(tx)));

        Page<TransactionResponseDto> result = transactionService.getByAccountId(
            1L, 1L, false, null, null, null, null, null, Pageable.unpaged()
        );

        assertEquals(1, result.getTotalElements());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getByAccountId_shouldFilterByType_whenTypeProvided() {

        BankAccount account = new BankAccount();
        account.setBalance(BigDecimal.valueOf(1000.0));

        Transaction tx = new Transaction(account, BigDecimal.valueOf(200.0), TransactionType.DEPOSIT);

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(tx)));

        Page<TransactionResponseDto> result = transactionService.getByAccountId(
            1L, 99L, true, TransactionType.DEPOSIT, null, null, null, null, Pageable.unpaged()
        );

        assertEquals(1, result.getTotalElements());
        assertEquals(TransactionType.DEPOSIT, result.getContent().get(0).getType());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getByAccountId_shouldFilterByDateRange() {

        BankAccount account = new BankAccount();
        account.setBalance(BigDecimal.valueOf(1000.0));

        Transaction tx = new Transaction(account, BigDecimal.valueOf(200.0), TransactionType.WITHDRAW);

        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(tx)));

        Page<TransactionResponseDto> result = transactionService.getByAccountId(
            1L, 99L, true, null, from, to, null, null, Pageable.unpaged()
        );

        assertEquals(1, result.getTotalElements());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getByAccountId_shouldFilterByAmountRange() {

        BankAccount account = new BankAccount();
        account.setBalance(BigDecimal.valueOf(1000.0));

        Transaction tx = new Transaction(account, BigDecimal.valueOf(500.0), TransactionType.DEPOSIT);

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(tx)));

        Page<TransactionResponseDto> result = transactionService.getByAccountId(
            1L, 99L, true, null, null, null, BigDecimal.valueOf(100.0), BigDecimal.valueOf(1000.0), Pageable.unpaged()
        );

        assertEquals(1, result.getTotalElements());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getByAccountId_shouldThrowException_whenNotOwner() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(1L);

        User user = new User();
        user.setAppUser(appUser);

        BankAccount account = new BankAccount();
        account.setUser(user);

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThrows(UnauthorizedActionException.class, () ->
            transactionService.getByAccountId(
                1L, 99L, false, null, null, null, null, null, Pageable.unpaged()
            )
        );

        verify(transactionRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }
}