package com.banking.system.service;

import com.banking.system.dto.request.BankAccountCreateRequestDto;
import com.banking.system.dto.response.AuditLogResponseDto;
import com.banking.system.dto.response.BankAccountResponseDto;
import com.banking.system.exception.AccountLimitExceedException;
import com.banking.system.exception.AccountNotActiveException;
import com.banking.system.exception.InvalidAccountStateException;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.exception.UnauthorizedActionException;
import com.banking.system.model.entities.AppUser;
import com.banking.system.model.entities.BankAccount;
import com.banking.system.model.entities.User;
import com.banking.system.model.enums.AccountStatus;
import com.banking.system.model.enums.ActionType;
import com.banking.system.repository.BankAccountRepository;
import com.banking.system.repository.UserRepository;
import com.banking.system.services.AuditLogService;
import com.banking.system.services.BankAccountService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BankAccountServiceTests {
    
    @Mock
    private AuditLogService auditLogService;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BankAccountService bankAccountService;

    // Create bank account success
    @Test
    void createBankAccount_shouldCreate_WhenAppUserExist_andBankAccount_doesntExceedLimit() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId())
            .thenReturn(5L);

        User user = mock(User.class);
        when(user.getId())
            .thenReturn(1L);

        when(user.getAppUser())
            .thenReturn(appUser);

        when(userRepository.findById(1L))
            .thenReturn(Optional.of(user));

        when(bankAccountRepository.countByUser_Id(anyLong()))
            .thenReturn(1L);

        when(bankAccountRepository.save(any(BankAccount.class)))
            .thenAnswer(i -> i.getArgument(0));

        when(auditLogService.logAction(
            eq(99L),
            eq(5L),
            eq(ActionType.CREATE_ACCOUNT)
        )).thenReturn(new AuditLogResponseDto(
            1L,
            99L,
            5L,
            ActionType.CREATE_ACCOUNT,
            LocalDateTime.now()
        ));

        BankAccountCreateRequestDto request = new BankAccountCreateRequestDto();
        request.setUserId(1L);

        BankAccountResponseDto result = bankAccountService.createAccount(request, 99L);

        assertEquals(0, BigDecimal.ZERO.compareTo(result.getBalance()));
        assertEquals(AccountStatus.ACTIVE, result.getStatus());

        verify(bankAccountRepository, times(1))
            .save(any(BankAccount.class));

        verify(auditLogService, times(1))
            .logAction(
                99L, 
                5L, 
                ActionType.CREATE_ACCOUNT
            );
    }

    // Create account user not found
    @Test
    void createAccount_shouldThrowException_whenUserNotFound() {
        
        when(userRepository.findById(1L))
            .thenReturn(Optional.empty());

        BankAccountCreateRequestDto request = new BankAccountCreateRequestDto();
        request.setUserId(1L);

        assertThrows(ResourceNotFoundException.class, () -> 
            bankAccountService.createAccount(request, 99L)
        );

        verify(bankAccountRepository, never())
            .save(any());
    }

    // Create account limit exceeded
    @Test
    void createAccount_shouldThrowException_whenAccountLimitExceeded() {

        User user = mock(User.class);
        when(user.getId())
            .thenReturn(1L);

        when(userRepository.findById(1L))
            .thenReturn(Optional.of(user));

        when(bankAccountRepository.countByUser_Id(1L))
            .thenReturn(3L);

        BankAccountCreateRequestDto request = new BankAccountCreateRequestDto();
        request.setUserId(1L);

        assertThrows(AccountLimitExceedException.class, () -> 
            bankAccountService.createAccount(request, 99L)
        );

        verify(bankAccountRepository, never())
            .save(any());
    }

    // Get by id - owner
    @Test
    void getBankAccountById_shouldReturnAccount_whenOwner() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId())
            .thenReturn(5L);

        User user = mock(User.class);
        when(user.getId())
            .thenReturn(1L);
        
        when(user.getAppUser())
            .thenReturn(appUser);

        BankAccount account = new BankAccount(
            user,
            BigDecimal.valueOf(500.0),
            AccountStatus.ACTIVE
        );

        when(bankAccountRepository.findById(10L))
            .thenReturn(Optional.of(account));

        BankAccountResponseDto result = bankAccountService.getBankAccountById(
            10L, 
            5L, 
            false
        );

        assertEquals(0, BigDecimal.valueOf(500.0).compareTo(result.getBalance()));
    }

    // Get by id - admin'
    @Test
    void getBankAccountById_shouldReturnAccount_whenAdmin() {

        User user = mock(User.class);
        when(user.getId())
            .thenReturn(1L);

        BankAccount account = new BankAccount(
            user,
            BigDecimal.valueOf(500.0),
            AccountStatus.ACTIVE
        );

        when(bankAccountRepository.findById(10L))
            .thenReturn(Optional.of(account));

        BankAccountResponseDto result = bankAccountService.getBankAccountById(10L, 999L, true);

        assertEquals(0, BigDecimal.valueOf(500.0).compareTo(result.getBalance()));
    }

    // Get by id - not owner
    @Test
    void getBankAccountById_shouldThrowException_whenNotOwner() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId())
            .thenReturn(5L);

        User user = mock(User.class);
        when(user.getAppUser())
            .thenReturn(appUser);

        BankAccount account = new BankAccount(
            user,
            BigDecimal.valueOf(500.0),
            AccountStatus.ACTIVE
        );

        when(bankAccountRepository.findById(10L))
            .thenReturn(Optional.of(account));

        assertThrows(UnauthorizedActionException.class, () -> 
            bankAccountService.getBankAccountById(10L, 999L, false)
        );
    }

    // Get by id - not found
    @Test
    void getBankAccountById_shouldThrowException_whenNotFound() {

        when(bankAccountRepository.findById(10L))
            .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
            bankAccountService.getBankAccountById(10L, 5L, false)
        );
    }

    // Get by user id
    @Test
    void getByUserId_shouldReturnPagedAccounts() {

        User user = mock(User.class);
        when(user.getId())
            .thenReturn(1L);

        BankAccount account = new BankAccount(
            user,
            BigDecimal.valueOf(500.0),
            AccountStatus.ACTIVE 
        );

        when(userRepository.findById(1L))
            .thenReturn(Optional.of(user));

        when(bankAccountRepository.findByUser_Id(eq(1L), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(account)));

        Page<BankAccountResponseDto> result = bankAccountService.getByUserId(
            1L, 
            Pageable.unpaged()
        );

        assertEquals(1, result.getTotalElements());
    }

    // Close
    @Test
    void closeAccount_shouldCloseAccount_andLogAction() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId())
            .thenReturn(5L);

        User user = mock(User.class);
        when(user.getAppUser())
            .thenReturn(appUser);

        BankAccount account = new BankAccount(
            user,
            BigDecimal.valueOf(500.0),
            AccountStatus.ACTIVE
        );

        when(bankAccountRepository.findById(10L))
            .thenReturn(Optional.of(account));

        when(bankAccountRepository.save(any(BankAccount.class)))
            .thenAnswer(i -> i.getArgument(0));

        when(auditLogService.logAction(
            eq(99L), 
            eq(5L), 
            eq(ActionType.CLOSE_ACCOUNT)
        )).thenReturn(new AuditLogResponseDto(
            1L, 
            99L, 
            5L, 
            ActionType.CLOSE_ACCOUNT, 
            LocalDateTime.now()));

        BankAccountResponseDto result = bankAccountService.closeAccount(
            10L, 
            99L,
            true
        );

        assertEquals(AccountStatus.CLOSED, result.getStatus());

        verify(auditLogService, times(1))
            .logAction(
                99L, 
                5L, 
                ActionType.CLOSE_ACCOUNT
            );
    }

    // Freeze account
    @Test
    void freezeAccount_shouldFreezeAccount_andLogAction() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId())
            .thenReturn(5L);

        User user = mock(User.class);
        when(user.getAppUser())
            .thenReturn(appUser);

        BankAccount account = new BankAccount(
            user,
            BigDecimal.valueOf(500.0),
            AccountStatus.ACTIVE
        );

        when(bankAccountRepository.findById(10L))
            .thenReturn(Optional.of(account));

        when(bankAccountRepository.save(any(BankAccount.class)))
            .thenAnswer(i -> i.getArgument(0));

        when(auditLogService.logAction(
            eq(99L), 
            eq(5L), 
            eq(ActionType.FREEZE_ACCOUNT)
        )).thenReturn(new AuditLogResponseDto(
            1L,
            99L,
            5L,
            ActionType.FREEZE_ACCOUNT,
            LocalDateTime.now()
        ));

        BankAccountResponseDto result = bankAccountService.freezeAccount(
            10L, 
            99L, 
            true
        );

        assertEquals(AccountStatus.FROZEN, result.getStatus());

        verify(auditLogService, times(1))
            .logAction(
                eq(99L), 
                eq(5L), 
                eq(ActionType.FREEZE_ACCOUNT)
            );
    }

    // Freeze account - already closed
    @Test
    void freezeAccount_shouldThrowException_whenAccountClosed() {

        User user = mock(User.class);

        BankAccount account = new BankAccount(
            user,
            BigDecimal.valueOf(500.0),
            AccountStatus.CLOSED
        );

        when(bankAccountRepository.findById(10L))
            .thenReturn(Optional.of(account));

        assertThrows(InvalidAccountStateException.class, () -> 
            bankAccountService.freezeAccount(10L, 99L, true)
        );

        verify(bankAccountRepository, never())
            .save(any());
    }


    // Unfreeze account
    @Test
    void unfreezeAccount_shouldUnfreezeAccount_andLogAction() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId())
            .thenReturn(5L);

        User user = mock(User.class);
        when(user.getAppUser())
            .thenReturn(appUser);

        BankAccount account = new BankAccount(
            user,
            BigDecimal.valueOf(500.0),
            AccountStatus.FROZEN
        );

        when(bankAccountRepository.findById(10L))
            .thenReturn(Optional.of(account));

        when(bankAccountRepository.save(any(BankAccount.class)))
            .thenAnswer(i -> i.getArgument(0));

        when(auditLogService.logAction(
            eq(99L), 
            eq(5L), 
            eq(ActionType.UNFREEZE_ACCOUNT)
        )).thenReturn(new AuditLogResponseDto(
            1L,
            99L,
            5L,
            ActionType.UNFREEZE_ACCOUNT,
            LocalDateTime.now()
        ));

        BankAccountResponseDto result = bankAccountService.unfreezeAccount(
            10L, 
            99L, 
            true
        );

        assertEquals(AccountStatus.ACTIVE, result.getStatus());

        verify(auditLogService, times(1))
            .logAction(
                eq(99L), 
                eq(5L), 
                eq(ActionType.UNFREEZE_ACCOUNT)
            );
    }

    // Unfreeze account - already closed
    @Test
    void unfreezeAccount_shouldThrowException_whenAccountClosed() {

        User user = mock(User.class);

        BankAccount account = new BankAccount(
            user,
            BigDecimal.valueOf(500.0),
            AccountStatus.CLOSED
        );

        when(bankAccountRepository.findById(10L))
            .thenReturn(Optional.of(account));

        assertThrows(InvalidAccountStateException.class, () -> 
            bankAccountService.unfreezeAccount(10L, 99L, true)
        );

        verify(bankAccountRepository, never())
            .save(any());
    }

    // Validate active - active account passes
    @Test
    void validateActive_shouldNotThrow_whenAccountActive() {

        User user = mock(User.class);

        BankAccount account = new BankAccount(
            user,
            BigDecimal.valueOf(500.0),
            AccountStatus.ACTIVE
        );

        assertDoesNotThrow(() -> 
        bankAccountService.validateActive(account));
    }

    // Validate active - frozen account throws
    @Test
    void validateActive_shouldThrowException_whenAccountFrozen() {

        User user = mock(User.class);

        BankAccount account = new BankAccount(
            user,
            BigDecimal.valueOf(500.0),
            AccountStatus.FROZEN
        );

        assertThrows(AccountNotActiveException.class, () ->
            bankAccountService.validateActive(account)
        );
    }

    // Validate active - closed account throws
    @Test
    void validateActive_shouldThrowException_whenAccountClosed() {

        User user = mock(User.class);

        BankAccount account = new BankAccount(
            user,
            BigDecimal.valueOf(500.0),
            AccountStatus.CLOSED
        );

        assertThrows(AccountNotActiveException.class, () -> 
            bankAccountService.validateActive(account)
        );
    }
}
