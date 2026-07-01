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
import com.banking.system.model.entities.JointAccountMember;
import com.banking.system.model.entities.User;
import com.banking.system.model.enums.AccountStatus;
import com.banking.system.model.enums.ActionType;
import com.banking.system.model.enums.JointAccountRole;
import com.banking.system.repository.BankAccountRepository;
import com.banking.system.repository.JointAccountMemberRepository;
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
import org.springframework.data.jpa.domain.Specification;

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

    @Mock
    private JointAccountMemberRepository jointAccountMemberRepository;

    @InjectMocks
    private BankAccountService bankAccountService;


    // ===================== CREATE =====================

    @Test
    void createBankAccount_shouldCreate_andRegisterPrimaryMember_whenUserExists_andUnderLimit() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(5L);

        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getAppUser()).thenReturn(appUser);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bankAccountRepository.countByUser_Id(anyLong())).thenReturn(1L);
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(i -> i.getArgument(0));
        when(jointAccountMemberRepository.save(any(JointAccountMember.class))).thenAnswer(i -> i.getArgument(0));

        when(auditLogService.logAction(eq(99L), eq(5L), eq(ActionType.CREATE_ACCOUNT)))
            .thenReturn(new AuditLogResponseDto(1L, 99L, 5L, ActionType.CREATE_ACCOUNT, LocalDateTime.now()));

        BankAccountCreateRequestDto request = new BankAccountCreateRequestDto();
        request.setUserId(1L);

        BankAccountResponseDto result = bankAccountService.createAccount(request, 99L);

        assertEquals(0, BigDecimal.ZERO.compareTo(result.getBalance()));
        assertEquals(AccountStatus.ACTIVE, result.getStatus());
        assertFalse(result.getIsJoint());

        verify(bankAccountRepository, times(1)).save(any(BankAccount.class));
        // PRIMARY member must be registered after account is created
        verify(jointAccountMemberRepository, times(1)).save(any(JointAccountMember.class));
        verify(auditLogService, times(1)).logAction(99L, 5L, ActionType.CREATE_ACCOUNT);
    }

    @Test
    void createBankAccount_shouldThrowException_whenUserNotFound() {

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        BankAccountCreateRequestDto request = new BankAccountCreateRequestDto();
        request.setUserId(1L);

        assertThrows(ResourceNotFoundException.class, () ->
            bankAccountService.createAccount(request, 99L)
        );

        verify(bankAccountRepository, never()).save(any());
        verify(jointAccountMemberRepository, never()).save(any());
    }

    @Test
    void createBankAccount_shouldThrowException_whenAccountLimitExceeded() {

        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bankAccountRepository.countByUser_Id(1L)).thenReturn(3L);

        BankAccountCreateRequestDto request = new BankAccountCreateRequestDto();
        request.setUserId(1L);

        assertThrows(AccountLimitExceedException.class, () ->
            bankAccountService.createAccount(request, 99L)
        );

        verify(bankAccountRepository, never()).save(any());
        verify(jointAccountMemberRepository, never()).save(any());
    }


    // ===================== GET BY ID =====================

    @Test
    void getBankAccountById_shouldReturnAccount_whenPrimaryOwner() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(5L);

        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getAppUser()).thenReturn(appUser);

        BankAccount account = new BankAccount(user, BigDecimal.valueOf(500.0), AccountStatus.ACTIVE);

        JointAccountMember primaryMember = mock(JointAccountMember.class);
        when(primaryMember.getUser()).thenReturn(user);

        when(bankAccountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(jointAccountMemberRepository.findByBankAccount_Id(any()))
            .thenReturn(List.of(primaryMember));

        BankAccountResponseDto result = bankAccountService.getBankAccountById(10L, 5L, false);

        assertEquals(0, BigDecimal.valueOf(500.0).compareTo(result.getBalance()));
    }

    @Test
    void getBankAccountById_shouldReturnAccount_whenCoOwner() {

        AppUser primaryAppUser = mock(AppUser.class);
        when(primaryAppUser.getId()).thenReturn(5L);

        User primaryUser = mock(User.class);
        when(primaryUser.getAppUser()).thenReturn(primaryAppUser);

        AppUser coOwnerAppUser = mock(AppUser.class);
        when(coOwnerAppUser.getId()).thenReturn(7L);

        User coOwnerUser = mock(User.class);
        when(coOwnerUser.getAppUser()).thenReturn(coOwnerAppUser);

        BankAccount account = new BankAccount(primaryUser, BigDecimal.valueOf(500.0), AccountStatus.ACTIVE);

        JointAccountMember primaryMember = mock(JointAccountMember.class);
        when(primaryMember.getUser()).thenReturn(primaryUser);

        JointAccountMember coOwnerMember = mock(JointAccountMember.class);
        when(coOwnerMember.getUser()).thenReturn(coOwnerUser);

        when(bankAccountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(jointAccountMemberRepository.findByBankAccount_Id(any()))
            .thenReturn(List.of(primaryMember, coOwnerMember));

        BankAccountResponseDto result = bankAccountService.getBankAccountById(10L, 7L, false);

        assertEquals(0, BigDecimal.valueOf(500.0).compareTo(result.getBalance()));
    }

    @Test
    void getBankAccountById_shouldReturnAccount_whenAdmin() {

        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        BankAccount account = new BankAccount(user, BigDecimal.valueOf(500.0), AccountStatus.ACTIVE);

        when(bankAccountRepository.findById(10L)).thenReturn(Optional.of(account));

        BankAccountResponseDto result = bankAccountService.getBankAccountById(10L, 999L, true);

        assertEquals(0, BigDecimal.valueOf(500.0).compareTo(result.getBalance()));
        // Admin bypasses member check entirely
        verify(jointAccountMemberRepository, never()).findByBankAccount_Id(any());
    }

    @Test
    void getBankAccountById_shouldThrowException_whenNotMember() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(5L);

        User user = mock(User.class);
        when(user.getAppUser()).thenReturn(appUser);

        BankAccount account = new BankAccount(user, BigDecimal.valueOf(500.0), AccountStatus.ACTIVE);

        JointAccountMember primaryMember = mock(JointAccountMember.class);
        when(primaryMember.getUser()).thenReturn(user);

        when(bankAccountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(jointAccountMemberRepository.findByBankAccount_Id(any()))
            .thenReturn(List.of(primaryMember));

        assertThrows(UnauthorizedActionException.class, () ->
            bankAccountService.getBankAccountById(10L, 999L, false)
        );
    }

    @Test
    void getBankAccountById_shouldThrowException_whenNotFound() {

        when(bankAccountRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            bankAccountService.getBankAccountById(10L, 5L, false)
        );
    }


    // ===================== GET BY USER ID (with optional status filter) =====================

    @SuppressWarnings("unchecked")
    @Test
    void getByUserId_shouldReturnAllAccounts_whenNoStatusFilter() {

        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        BankAccount account = new BankAccount(user, BigDecimal.valueOf(500.0), AccountStatus.ACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bankAccountRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(account)));

        Page<BankAccountResponseDto> result = bankAccountService.getByUserId(1L, null, Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getByUserId_shouldFilterByStatus_whenStatusProvided() {

        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        BankAccount account = new BankAccount(user, BigDecimal.valueOf(500.0), AccountStatus.FROZEN);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bankAccountRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(account)));

        Page<BankAccountResponseDto> result = bankAccountService.getByUserId(1L, AccountStatus.FROZEN, Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        assertEquals(AccountStatus.FROZEN, result.getContent().get(0).getStatus());
    }

    @Test
    void getByUserId_shouldThrowException_whenUserNotFound() {

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            bankAccountService.getByUserId(1L, null, Pageable.unpaged())
        );
    }


    // ===================== GET ALL ACCOUNTS (admin, with optional status filter) =====================

    @SuppressWarnings("unchecked")
    @Test
    void getAllAccounts_shouldReturnAllAccounts_whenNoStatusFilter() {

        User user = mock(User.class);
        BankAccount account = new BankAccount(user, BigDecimal.valueOf(100.0), AccountStatus.ACTIVE);

        when(bankAccountRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(account)));

        Page<BankAccountResponseDto> result = bankAccountService.getAllAccounts(null, Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getAllAccounts_shouldFilterByStatus_whenStatusProvided() {

        User user = mock(User.class);
        BankAccount account = new BankAccount(user, BigDecimal.valueOf(100.0), AccountStatus.CLOSED);

        when(bankAccountRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(account)));

        Page<BankAccountResponseDto> result = bankAccountService.getAllAccounts(AccountStatus.CLOSED, Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        assertEquals(AccountStatus.CLOSED, result.getContent().get(0).getStatus());
    }


    // ===================== CLOSE =====================

    @Test
    void closeAccount_shouldCloseAccount_andLogAction_whenAdmin() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(5L);

        User user = mock(User.class);
        when(user.getAppUser()).thenReturn(appUser);

        BankAccount account = new BankAccount(user, BigDecimal.valueOf(500.0), AccountStatus.ACTIVE);

        when(bankAccountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(i -> i.getArgument(0));

        when(auditLogService.logAction(eq(99L), eq(5L), eq(ActionType.CLOSE_ACCOUNT)))
            .thenReturn(new AuditLogResponseDto(1L, 99L, 5L, ActionType.CLOSE_ACCOUNT, LocalDateTime.now()));

        BankAccountResponseDto result = bankAccountService.closeAccount(10L, 99L, true);

        assertEquals(AccountStatus.CLOSED, result.getStatus());
        verify(auditLogService, times(1)).logAction(99L, 5L, ActionType.CLOSE_ACCOUNT);
        // Admin bypasses PRIMARY check
        verify(jointAccountMemberRepository, never()).findByBankAccount_IdAndRole(any(), any());
    }

    @Test
    void closeAccount_shouldCloseAccount_whenPrimaryOwner() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(5L);

        User user = mock(User.class);
        when(user.getAppUser()).thenReturn(appUser);

        BankAccount account = new BankAccount(user, BigDecimal.valueOf(500.0), AccountStatus.ACTIVE);

        JointAccountMember primary = mock(JointAccountMember.class);
        when(primary.getUser()).thenReturn(user);

        when(bankAccountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(jointAccountMemberRepository.findByBankAccount_IdAndRole(10L, JointAccountRole.PRIMARY))
            .thenReturn(Optional.of(primary));
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(i -> i.getArgument(0));

        BankAccountResponseDto result = bankAccountService.closeAccount(10L, 5L, false);

        assertEquals(AccountStatus.CLOSED, result.getStatus());
        // Non-admin close does not log audit
        verify(auditLogService, never()).logAction(any(), any(), any());
    }

    @Test
    void closeAccount_shouldThrowException_whenCoOwnerTriesToClose() {

        AppUser primaryAppUser = mock(AppUser.class);
        when(primaryAppUser.getId()).thenReturn(5L);

        User primaryUser = mock(User.class);
        when(primaryUser.getAppUser()).thenReturn(primaryAppUser);

        BankAccount account = new BankAccount(primaryUser, BigDecimal.valueOf(500.0), AccountStatus.ACTIVE);

        JointAccountMember primary = mock(JointAccountMember.class);
        when(primary.getUser()).thenReturn(primaryUser);

        when(bankAccountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(jointAccountMemberRepository.findByBankAccount_IdAndRole(10L, JointAccountRole.PRIMARY))
            .thenReturn(Optional.of(primary));

        // appUserId 99L is co-owner, not primary (5L)
        assertThrows(UnauthorizedActionException.class, () ->
            bankAccountService.closeAccount(10L, 99L, false)
        );

        verify(bankAccountRepository, never()).save(any());
        verify(auditLogService, never()).logAction(any(), any(), any());
    }

    @Test
    void closeAccount_shouldThrowException_whenNotFound() {

        when(bankAccountRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            bankAccountService.closeAccount(10L, 99L, true)
        );
    }


    // ===================== FREEZE =====================

    @Test
    void freezeAccount_shouldFreezeAccount_andLogAction() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(5L);

        User user = mock(User.class);
        when(user.getAppUser()).thenReturn(appUser);

        BankAccount account = new BankAccount(user, BigDecimal.valueOf(500.0), AccountStatus.ACTIVE);

        when(bankAccountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(i -> i.getArgument(0));

        when(auditLogService.logAction(eq(99L), eq(5L), eq(ActionType.FREEZE_ACCOUNT)))
            .thenReturn(new AuditLogResponseDto(1L, 99L, 5L, ActionType.FREEZE_ACCOUNT, LocalDateTime.now()));

        BankAccountResponseDto result = bankAccountService.freezeAccount(10L, 99L, true);

        assertEquals(AccountStatus.FROZEN, result.getStatus());
        verify(auditLogService, times(1)).logAction(eq(99L), eq(5L), eq(ActionType.FREEZE_ACCOUNT));
    }

    @Test
    void freezeAccount_shouldThrowException_whenAccountClosed() {

        User user = mock(User.class);
        BankAccount account = new BankAccount(user, BigDecimal.valueOf(500.0), AccountStatus.CLOSED);

        when(bankAccountRepository.findById(10L)).thenReturn(Optional.of(account));

        assertThrows(InvalidAccountStateException.class, () ->
            bankAccountService.freezeAccount(10L, 99L, true)
        );

        verify(bankAccountRepository, never()).save(any());
    }


    // ===================== UNFREEZE =====================

    @Test
    void unfreezeAccount_shouldUnfreezeAccount_andLogAction() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(5L);

        User user = mock(User.class);
        when(user.getAppUser()).thenReturn(appUser);

        BankAccount account = new BankAccount(user, BigDecimal.valueOf(500.0), AccountStatus.FROZEN);

        when(bankAccountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(i -> i.getArgument(0));

        when(auditLogService.logAction(eq(99L), eq(5L), eq(ActionType.UNFREEZE_ACCOUNT)))
            .thenReturn(new AuditLogResponseDto(1L, 99L, 5L, ActionType.UNFREEZE_ACCOUNT, LocalDateTime.now()));

        BankAccountResponseDto result = bankAccountService.unfreezeAccount(10L, 99L, true);

        assertEquals(AccountStatus.ACTIVE, result.getStatus());
        verify(auditLogService, times(1)).logAction(eq(99L), eq(5L), eq(ActionType.UNFREEZE_ACCOUNT));
    }

    @Test
    void unfreezeAccount_shouldThrowException_whenAccountClosed() {

        User user = mock(User.class);
        BankAccount account = new BankAccount(user, BigDecimal.valueOf(500.0), AccountStatus.CLOSED);

        when(bankAccountRepository.findById(10L)).thenReturn(Optional.of(account));

        assertThrows(InvalidAccountStateException.class, () ->
            bankAccountService.unfreezeAccount(10L, 99L, true)
        );

        verify(bankAccountRepository, never()).save(any());
    }


    // ===================== VALIDATE ACTIVE =====================

    @Test
    void validateActive_shouldNotThrow_whenAccountActive() {

        User user = mock(User.class);
        BankAccount account = new BankAccount(user, BigDecimal.valueOf(500.0), AccountStatus.ACTIVE);

        assertDoesNotThrow(() -> bankAccountService.validateActive(account));
    }

    @Test
    void validateActive_shouldThrowException_whenAccountFrozen() {

        User user = mock(User.class);
        BankAccount account = new BankAccount(user, BigDecimal.valueOf(500.0), AccountStatus.FROZEN);

        assertThrows(AccountNotActiveException.class, () ->
            bankAccountService.validateActive(account)
        );
    }

    @Test
    void validateActive_shouldThrowException_whenAccountClosed() {

        User user = mock(User.class);
        BankAccount account = new BankAccount(user, BigDecimal.valueOf(500.0), AccountStatus.CLOSED);

        assertThrows(AccountNotActiveException.class, () ->
            bankAccountService.validateActive(account)
        );
    }
}