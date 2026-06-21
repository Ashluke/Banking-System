package com.banking.system.services;

import com.banking.system.dto.request.BankAccountCreateRequestDto;
import com.banking.system.dto.response.BankAccountResponseDto;
import com.banking.system.exception.AccountLimitExceedException;
import com.banking.system.exception.AccountNotActiveException;
import com.banking.system.exception.InvalidAccountStateException;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.exception.UnauthorizedActionException;
import com.banking.system.model.entities.BankAccount;
import com.banking.system.model.entities.User;
import com.banking.system.model.enums.AccountStatus;
import com.banking.system.model.enums.ActionType;
import com.banking.system.repository.BankAccountRepository;
import com.banking.system.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class BankAccountService {
   
    private final AuditLogService auditLogService;
    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;

    public BankAccountService(AuditLogService auditLogService, BankAccountRepository bankAccountRepository, UserRepository userRepository) {
        this.auditLogService = auditLogService;
        this.bankAccountRepository = bankAccountRepository;
        this.userRepository = userRepository;
    }

    // Create bank account
    public BankAccountResponseDto createAccount(BankAccountCreateRequestDto request, Long appUserId) {
        User user = userRepository.findById(request.getUserId()).orElseThrow(() -> 
            new ResourceNotFoundException("AppUser not found"));

        long accountCount = bankAccountRepository.countByUser_Id(user.getId());

        if (accountCount >= 3) {
            throw new AccountLimitExceedException("Maximum of 3 bank accounts is allowed per user");
        }

        BankAccount account = new BankAccount();

        account.setUser(user);
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);

        BankAccount saved = bankAccountRepository.save(account);

        auditLogService.logAction(
            appUserId, 
            user.getAppUser().getId(), 
            ActionType.CREATE_ACCOUNT
        );

        return mapToResponse(saved);
    }

    // Get by id
    public BankAccountResponseDto getBankAccountById(Long id, Long appUserId, boolean isAdmin) {
        
        BankAccount account = bankAccountRepository.findById(id).orElseThrow(() ->
            new ResourceNotFoundException("Account not found"));

        checkOwnershipOrAdmin(account, appUserId, isAdmin);

        return mapToResponse(account);
    }

    // Get by user id
    public Page<BankAccountResponseDto> getByUserId(Long userId, Pageable pageable) {

        User user = userRepository.findById(userId).orElseThrow(() -> 
            new ResourceNotFoundException("User not found"));

        return bankAccountRepository.findByUser_Id(user.getId(), pageable)
        .map(this::mapToResponse);
    }

    // Close account
    public BankAccountResponseDto closeAccount(Long accountId, Long appUserId, boolean isAdmin) {

        BankAccount account = bankAccountRepository.findById(accountId).orElseThrow(() ->
            new ResourceNotFoundException("Account not found"));

        account.setStatus(AccountStatus.CLOSED);

        BankAccount updated = bankAccountRepository.save(account);

        if (isAdmin) {
            auditLogService.logAction(
                appUserId, 
                account.getUser().getAppUser().getId(), 
                ActionType.CLOSE_ACCOUNT
            );
        }

        return mapToResponse(updated);
    }

    // Freeze account
    public BankAccountResponseDto freezeAccount(Long accountId, Long appUserId, boolean isAdmin) {

        BankAccount account = bankAccountRepository.findById(accountId).orElseThrow(() -> 
            new ResourceNotFoundException("Account not found"));

        checkOwnershipOrAdmin(account, appUserId, isAdmin);

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new InvalidAccountStateException("Closed accounts cannot be frozen");
        }

        account.setStatus(AccountStatus.FROZEN);

        BankAccount updated = bankAccountRepository.save(account);

        if(isAdmin) {
            auditLogService.logAction(
                appUserId, 
                account.getUser().getAppUser().getId(), 
                ActionType.FREEZE_ACCOUNT
            );
        }

        return mapToResponse(updated);
    }

    // Unfreeze account
    public BankAccountResponseDto unfreezeAccount(Long accountId, Long appUserId, boolean isAdmin) {

        BankAccount account = bankAccountRepository.findById(accountId).orElseThrow(() -> 
            new ResourceNotFoundException("Account not found"));

        checkOwnershipOrAdmin(account, appUserId, isAdmin);

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new InvalidAccountStateException("Closed accounts cannot be unfrozen");
        }

        account.setStatus(AccountStatus.ACTIVE);

        BankAccount updated = bankAccountRepository.save(account);

        if(isAdmin) {
            auditLogService.logAction(
                appUserId, 
                account.getUser().getAppUser().getId(), 
                ActionType.UNFREEZE_ACCOUNT
            );
        }

        return mapToResponse(updated);
    }

    // Validation
    public void validateActive(BankAccount account) {
        
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(account.getId());
        }
    }

    // check ownership
    private void checkOwnershipOrAdmin(BankAccount account, Long appUserId, boolean isAdmin) {

        if (isAdmin) return;

        User user = account.getUser();

        if(!user.getAppUser().getId().equals(appUserId)) {
            throw new UnauthorizedActionException("You do not own this account");
        }
    }

    // Mapper
    private BankAccountResponseDto mapToResponse(BankAccount account) {
        
        return new BankAccountResponseDto(
            account.getId(),
            account.getBalance(),
            account.getStatus(),
            account.getUser().getId()
        );
    }
}