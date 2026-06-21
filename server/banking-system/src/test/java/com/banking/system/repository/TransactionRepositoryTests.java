package com.banking.system.repository;

import com.banking.system.model.entities.AppUser;
import com.banking.system.model.entities.BankAccount;
import com.banking.system.model.entities.Transaction;
import com.banking.system.model.entities.User;
import com.banking.system.model.enums.AccountStatus;
import com.banking.system.model.enums.Role;
import com.banking.system.model.enums.TransactionType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class TransactionRepositoryTests {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppUserRepository appUserRepository;


    private BankAccount createAccount() {

        AppUser appUser = new AppUser();
        appUser.setEmail("test" + System.nanoTime() + "@example.com");
        appUser.setPasswordHash("hashed");
        appUser.setRole(Role.USER);
        appUser = appUserRepository.save(appUser);

        User user = new User();
        user.setAppUser(appUser);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPhoneNumber("0917" + System.nanoTime() % 1000000000L);
        user.setAddress("123 Main St");
        user = userRepository.save(user);

        BankAccount account = new BankAccount(user, BigDecimal.valueOf(1000.0), AccountStatus.ACTIVE);
        return bankAccountRepository.save(account);
    }


    @Test
    void findByBankAccount_Id_shouldReturnTransactions_forThatAccount() {

        BankAccount account = createAccount();
        BankAccount otherAccount = createAccount();

        Transaction tx1 = new Transaction(account, BigDecimal.valueOf(100.0), TransactionType.DEPOSIT);
        Transaction tx2 = new Transaction(account, BigDecimal.valueOf(50.0), TransactionType.WITHDRAW);
        Transaction tx3 = new Transaction(otherAccount, BigDecimal.valueOf(200.0), TransactionType.DEPOSIT);

        transactionRepository.save(tx1);
        transactionRepository.save(tx2);
        transactionRepository.save(tx3);

        Page<Transaction> result = transactionRepository.findByBankAccount_Id(
            account.getId(), PageRequest.of(0, 10)
        );

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
            .extracting(Transaction::getAmount)
            .containsExactlyInAnyOrder(BigDecimal.valueOf(100.0), BigDecimal.valueOf(50.0));
    }

    @Test
    void findByBankAccount_Id_shouldReturnEmptyPage_whenNoTransactions() {

        BankAccount account = createAccount();

        Page<Transaction> result = transactionRepository.findByBankAccount_Id(
            account.getId(), PageRequest.of(0, 10)
        );

        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void findByBankAccount_Id_shouldRespectPagination() {

        BankAccount account = createAccount();

        for (int i = 0; i < 5; i++) {
            transactionRepository.save(
                new Transaction(account, BigDecimal.valueOf(10.0 * (i + 1)), TransactionType.DEPOSIT)
            );
        }

        Page<Transaction> firstPage = transactionRepository.findByBankAccount_Id(
            account.getId(), PageRequest.of(0, 2)
        );

        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
    }

    @Test
    void save_shouldPersistTransaction_withGeneratedIdAndTimestamp() {

        BankAccount account = createAccount();

        Transaction tx = new Transaction(account, BigDecimal.valueOf(75.0), TransactionType.DEPOSIT);

        Transaction saved = transactionRepository.save(tx);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTimestamp()).isNotNull();
        assertThat(saved.getBankAccount().getId()).isEqualTo(account.getId());
    }

    @Test
    void findAll_shouldReturnPagedTransactions() {

        BankAccount account = createAccount();

        transactionRepository.save(new Transaction(account, BigDecimal.valueOf(10.0), TransactionType.DEPOSIT));
        transactionRepository.save(new Transaction(account, BigDecimal.valueOf(20.0), TransactionType.WITHDRAW));

        Page<Transaction> result = transactionRepository.findAll(PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }
}