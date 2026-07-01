package com.banking.system.services;

import com.banking.system.model.entities.BankAccount;
import com.banking.system.model.entities.Transaction;
import com.banking.system.model.enums.AccountStatus;
import com.banking.system.repository.BankAccountRepository;
import com.banking.system.repository.TransactionRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AccountSchedulerService {

    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;

    // Auto-freeze after 60 days of inactivity
    private static final int FREEZE_AFTER_DAYS = 60;

    // Auto-close after frozen for 60 days with no unfreeze
    private static final int CLOSE_AFTER_DAYS = 60;

    public AccountSchedulerService(BankAccountRepository bankAccountRepository, TransactionRepository transactionRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
    }

    // Runs every day at 1:00 AM
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void autoFreezeInactiveAccounts() {

        LocalDateTime cutoff = LocalDateTime.now().minusDays(FREEZE_AFTER_DAYS);

        List<BankAccount> activeAccounts = bankAccountRepository.findByStatus(AccountStatus.ACTIVE);

        for (BankAccount account : activeAccounts) {
            Optional<Transaction> lastTransaction = transactionRepository
                .findTopByBankAccount_IdOrderByTimestampDesc(account.getId());

            boolean isInactive = lastTransaction
                .map(tx -> tx.getTimestamp().isBefore(cutoff))
                .orElse(true); // no transactions at all = inactive

            if (isInactive) {
                account.setStatus(AccountStatus.FROZEN);
                bankAccountRepository.save(account);
            }
        }
    }

    // Runs every day at 2:00 AM
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void autoCloseInactiveAccounts() {

        LocalDateTime cutoff = LocalDateTime.now().minusDays(CLOSE_AFTER_DAYS);

        List<BankAccount> frozenAccounts = bankAccountRepository.findByStatus(AccountStatus.FROZEN);

        for (BankAccount account : frozenAccounts) {
            Optional<Transaction> lastTransaction = transactionRepository
                .findTopByBankAccount_IdOrderByTimestampDesc(account.getId());

            boolean isInactive = lastTransaction
                .map(tx -> tx.getTimestamp().isBefore(cutoff))
                .orElse(true); // no transactions at all = inactive

            if (isInactive) {
                account.setStatus(AccountStatus.CLOSED);
                bankAccountRepository.save(account);
            }
        }
    }
}