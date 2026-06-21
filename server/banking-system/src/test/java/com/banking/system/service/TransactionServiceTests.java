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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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


    // Deposit test
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

    // Withdraw test
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

    // Withdraw insufficient balance
    @Test
    void withdraw_shouldThrowException_whenInsufficientBalance() {

        BankAccount account = new BankAccount();
        account.setBalance(BigDecimal.valueOf(100.0));

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        doNothing().when(bankAccountService).validateActive(any());

        WithdrawRequestDto request = new WithdrawRequestDto();
        request.setAccountId(1L);
        request.setAmount(BigDecimal.valueOf(500.0));

        assertThrows(RuntimeException.class, () -> transactionService.withdraw(request));
    }

    // Transfer
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

    // Transfer insufficient balance
    @Test
    void transfer_shouldThrowException_whenInsufficientBalance() {

        BankAccount from = new BankAccount();
        from.setBalance(BigDecimal.valueOf(100.0));

        BankAccount to = new BankAccount();
        to.setBalance(BigDecimal.valueOf(500.0));

        when(bankAccountRepository.findById(1L))
            .thenReturn(Optional.of(from));
        when(bankAccountRepository.findById(2L))
            .thenReturn(Optional.of(to));

        doNothing().when(bankAccountService).validateActive(any());

        TransferRequestDto request = new TransferRequestDto();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(BigDecimal.valueOf(300.0));

        assertThrows(InsufficientBalanceException.class, () ->
            transactionService.transfer(request)
        );

        verify(transactionRepository, never()).saveAll(any());
    }

    // Transaction when frozen/closed
    @Test
    void allOperations_shouldThrowException_whenAccountNotActive() {

        BankAccount account = new BankAccount();
        account.setBalance(BigDecimal.valueOf(500.0));

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(bankAccountRepository.findById(2L)).thenReturn(Optional.of(account));

        doThrow(new AccountNotActiveException(account.getId()))
            .when(bankAccountService).validateActive(any());

        DepositRequestDto depositRequest = new DepositRequestDto();
        depositRequest.setAccountId(1L);
        depositRequest.setAmount(BigDecimal.valueOf(200.0));

        WithdrawRequestDto withdrawRequest = new WithdrawRequestDto();
        withdrawRequest.setAccountId(1L);
        withdrawRequest.setAmount(BigDecimal.valueOf(200.0));

        TransferRequestDto transferRequest = new TransferRequestDto();
        transferRequest.setFromAccountId(1L);
        transferRequest.setToAccountId(2L);
        transferRequest.setAmount(BigDecimal.valueOf(200.0));

        assertThrows(AccountNotActiveException.class, () ->
            transactionService.deposit(depositRequest));

        assertThrows(AccountNotActiveException.class, () ->
            transactionService.withdraw(withdrawRequest));

        assertThrows(AccountNotActiveException.class, () ->
            transactionService.transfer(transferRequest));

        verify(bankAccountRepository, never()).save(any());
        verify(bankAccountRepository, never()).saveAll(any());
        verify(transactionRepository, never()).save(any());
        verify(transactionRepository, never()).saveAll(any());
    }

    // Transfer to the same account
    @Test
    void transfer_shouldThrowException_whenSameAccount() {

        TransferRequestDto request = new TransferRequestDto();
        request.setFromAccountId(1L);
        request.setToAccountId(1L);
        request.setAmount(BigDecimal.valueOf(200.0));

        assertThrows(InvalidTransferException.class, () ->
            transactionService.transfer(request)
        );

        verify(bankAccountRepository, never()).findById(any());
        verify(transactionRepository, never()).saveAll(any());
    }

    // Account not found (all operations)
    @Test
    void allOperations_shouldThrowException_whenAccountNotFound() {

        BankAccount from = new BankAccount();
        from.setBalance(BigDecimal.valueOf(1000.0));

        // deposit / withdraw: account 1L not found
        // transfer (source not found): account 1L not found, 2L never reached
        // transfer (destination not found): account 1L found, 2L not found
        when(bankAccountRepository.findById(1L))
            .thenReturn(Optional.empty())   // deposit
            .thenReturn(Optional.empty())   // withdraw
            .thenReturn(Optional.empty())   // transfer source not found
            .thenReturn(Optional.of(from)); // transfer destination not found (source found this time)

        when(bankAccountRepository.findById(2L))
            .thenReturn(Optional.empty());  // transfer destination not found

        DepositRequestDto depositRequest = new DepositRequestDto();
        depositRequest.setAccountId(1L);
        depositRequest.setAmount(BigDecimal.valueOf(200.0));

        WithdrawRequestDto withdrawRequest = new WithdrawRequestDto();
        withdrawRequest.setAccountId(1L);
        withdrawRequest.setAmount(BigDecimal.valueOf(200.0));

        TransferRequestDto transferSourceMissing = new TransferRequestDto();
        transferSourceMissing.setFromAccountId(1L);
        transferSourceMissing.setToAccountId(2L);
        transferSourceMissing.setAmount(BigDecimal.valueOf(200.0));

        TransferRequestDto transferDestMissing = new TransferRequestDto();
        transferDestMissing.setFromAccountId(1L);
        transferDestMissing.setToAccountId(2L);
        transferDestMissing.setAmount(BigDecimal.valueOf(200.0));

        assertThrows(ResourceNotFoundException.class, () ->
            transactionService.deposit(depositRequest));

        assertThrows(ResourceNotFoundException.class, () ->
            transactionService.withdraw(withdrawRequest));

        assertThrows(ResourceNotFoundException.class, () ->
            transactionService.transfer(transferSourceMissing));

        assertThrows(ResourceNotFoundException.class, () ->
            transactionService.transfer(transferDestMissing));

        verify(bankAccountRepository, never()).save(any());
        verify(bankAccountRepository, never()).saveAll(any());
        verify(transactionRepository, never()).save(any());
        verify(transactionRepository, never()).saveAll(any());
    }

    // Get transactions when admin
    @Test
    void getByAccountId_shouldReturnTransactions_whenAdmin() {

        BankAccount account = new BankAccount();
        account.setBalance(BigDecimal.valueOf(1000.0));

        Transaction tx = new Transaction(
            account,
            BigDecimal.valueOf(200.0),
            TransactionType.DEPOSIT
        );

        when(bankAccountRepository.findById(1L))
            .thenReturn(Optional.of(account));

        when(transactionRepository.findByBankAccount_Id(
            eq(1L), any(Pageable.class))
        ).thenReturn(new PageImpl<>(List.of(tx)));

        Page<TransactionResponseDto> result = transactionService
            .getByAccountId(1L, 99L, true, Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        verify(transactionRepository, times(1))
            .findByBankAccount_Id(eq(1L), any(Pageable.class));
    }

    // Get transactions when owner
    @Test
    void getByAccountId_shouldReturnTransactions_whenOwner() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(1L);

        User user = new User();
        user.setAppUser(appUser);

        BankAccount account = new BankAccount();
        account.setBalance(BigDecimal.valueOf(1000.0));
        account.setUser(user);

        Transaction tx = new Transaction(
            account,
            BigDecimal.valueOf(200.0),
            TransactionType.DEPOSIT
        );

        when(bankAccountRepository.findById(1L))
            .thenReturn(Optional.of(account));

        when(transactionRepository.findByBankAccount_Id(
            eq(1L), any(Pageable.class))
        ).thenReturn(new PageImpl<>(List.of(tx)));

        Page<TransactionResponseDto> result = transactionService
            .getByAccountId(1L, 1L, false, Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        verify(transactionRepository, times(1))
            .findByBankAccount_Id(eq(1L), any(Pageable.class));
    }

    // Get transactions when not owner (should error)
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
            transactionService.getByAccountId(1L, 99L, false, Pageable.unpaged())
        );

        verify(transactionRepository, never()).findByBankAccount_Id(any(), any());
    }
}
