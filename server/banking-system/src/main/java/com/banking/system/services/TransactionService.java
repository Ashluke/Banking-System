package com.banking.system.services;

import com.banking.system.dto.request.DepositRequestDto;
import com.banking.system.dto.request.WithdrawRequestDto;
import com.banking.system.dto.request.TransferRequestDto;
import com.banking.system.dto.response.TransactionResponseDto;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.exception.UnauthorizedActionException;
import com.banking.system.exception.InsufficientBalanceException;
import com.banking.system.exception.InvalidTransferException;
import com.banking.system.model.entities.BankAccount;
import com.banking.system.model.entities.Transaction;
import com.banking.system.model.enums.TransactionType;
import com.banking.system.repository.BankAccountRepository;
import com.banking.system.repository.TransactionRepository;
import com.banking.system.specification.TransactionSpecification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {
    
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final BankAccountService bankAccountService;

    public TransactionService(BankAccountRepository bankAccountRepository,
            TransactionRepository transactionRepository,
            BankAccountService bankAccountService) {
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.bankAccountService = bankAccountService;
    }

    // Deposit
    @Transactional
    public TransactionResponseDto deposit(DepositRequestDto request) {

        BankAccount account = bankAccountRepository.findById(request.getAccountId()).orElseThrow(() -> 
            new ResourceNotFoundException("Account not found"));

        bankAccountService.validateActive(account);

        account.setBalance(account.getBalance().add(request.getAmount()));

        BankAccount savedAccount = bankAccountRepository.save(account);

        Transaction tx = new Transaction(
            savedAccount,
            request.getAmount(),
            TransactionType.DEPOSIT
        );

        Transaction savedTx = transactionRepository.save(tx);

        return mapToResponse_3args(savedTx);
    }

    // Withdraw
    @Transactional
    public TransactionResponseDto withdraw(WithdrawRequestDto request) {

        BankAccount account = bankAccountRepository.findById(request.getAccountId()).orElseThrow(() -> 
            new ResourceNotFoundException("Account not found"));

        bankAccountService.validateActive(account);

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));

        BankAccount savedAccount = bankAccountRepository.save(account);

        Transaction tx = new Transaction(
            savedAccount,
            request.getAmount(),
            TransactionType.WITHDRAW
        );

        Transaction savedTx = transactionRepository.save(tx);

        return mapToResponse_3args(savedTx);
    }

    // Transfer
    @Transactional
    public TransactionResponseDto transfer(TransferRequestDto request) {

        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new InvalidTransferException("Cannot transfer to the same account");
        }

        BankAccount from = bankAccountRepository.findById(request.getFromAccountId()).orElseThrow(() -> 
            new ResourceNotFoundException("Source account not found"));
            
        BankAccount to = bankAccountRepository.findById(request.getToAccountId()).orElseThrow(() -> 
            new ResourceNotFoundException("Destination account not found"));

        bankAccountService.validateActive(from);
        bankAccountService.validateActive(to);

        if (from.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        from.setBalance(from.getBalance().subtract(request.getAmount()));
        to.setBalance(to.getBalance().add(request.getAmount()));

        List<BankAccount> savedAccounts = bankAccountRepository.saveAll(List.of(from, to));
        BankAccount savedFrom = savedAccounts.get(0);
        BankAccount savedTo = savedAccounts.get(1);

        Transaction fromTx = new Transaction(
            savedFrom,
            null,
            request.getAmount(),
            TransactionType.TRANSFER_OUT
        );

        Transaction toTx = new Transaction(
            savedTo,
            null,
            request.getAmount(),
            TransactionType.TRANSFER_IN
        );

        transactionRepository.saveAll(List.of(fromTx, toTx));

        fromTx.setRelatedTransactionId(toTx.getId());
        toTx.setRelatedTransactionId(fromTx.getId());

        transactionRepository.saveAll(List.of(fromTx, toTx));

        return mapToResponse(fromTx);
    }

    // Get transactions by account id with filters
    public Page<TransactionResponseDto> getByAccountId(
            Long accountId,
            Long appUserId,
            boolean isAdmin,
            TransactionType type,
            LocalDateTime from,
            LocalDateTime to,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            Pageable pageable) {

        BankAccount account = bankAccountRepository.findById(accountId).orElseThrow(() -> 
            new ResourceNotFoundException("Account not found"));

        if (!isAdmin && !account.getUser().getAppUser().getId().equals(appUserId)) {
            throw new UnauthorizedActionException("You do not own this account");
        }

        Specification<Transaction> spec = Specification
            .where(TransactionSpecification.hasAccountId(accountId))
            .and(TransactionSpecification.hasType(type))
            .and(TransactionSpecification.afterDate(from))
            .and(TransactionSpecification.beforeDate(to))
            .and(TransactionSpecification.minAmount(minAmount))
            .and(TransactionSpecification.maxAmount(maxAmount));

        return transactionRepository.findAll(spec, pageable)
            .map(this::mapToResponse);
    }

    // Mapper (3 args)
    private TransactionResponseDto mapToResponse_3args(Transaction tx) {

        return new TransactionResponseDto(
            tx.getId(), 
            tx.getBankAccount().getId(), 
            tx.getAmount(), 
            tx.getType(), 
            tx.getTimestamp()
        );
    }

    // Mapper
    private TransactionResponseDto mapToResponse(Transaction tx) {

        return new TransactionResponseDto(
            tx.getId(),
            tx.getBankAccount().getId(),
            tx.getRelatedTransactionId(),
            tx.getAmount(),
            tx.getType(),
            tx.getTimestamp()
        );
    }
}