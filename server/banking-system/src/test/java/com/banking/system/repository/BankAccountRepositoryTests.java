package com.banking.system.repository;

import com.banking.system.model.entities.AppUser;
import com.banking.system.model.entities.BankAccount;
import com.banking.system.model.entities.User;
import com.banking.system.model.enums.AccountStatus;
import com.banking.system.model.enums.Role;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class BankAccountRepositoryTests {

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppUserRepository appUserRepository;


    private User createUser(String email, String phoneNumber) {

        AppUser appUser = new AppUser();
        appUser.setEmail(email);
        appUser.setPasswordHash("hashed");
        appUser.setRole(Role.USER);
        appUser = appUserRepository.save(appUser);

        User user = new User();
        user.setAppUser(appUser);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPhoneNumber(phoneNumber);
        user.setAddress("123 Main St");

        return userRepository.save(user);
    }


    @Test
    void countByUser_Id_shouldReturnCorrectCount() {

        User user = createUser("test1@example.com", "09171111111");

        bankAccountRepository.save(new BankAccount(user, BigDecimal.ZERO, AccountStatus.ACTIVE));
        bankAccountRepository.save(new BankAccount(user, BigDecimal.ZERO, AccountStatus.ACTIVE));

        long count = bankAccountRepository.countByUser_Id(user.getId());

        assertThat(count).isEqualTo(2);
    }

    @Test
    void countByUser_Id_shouldReturnZero_whenUserHasNoAccounts() {

        User user = createUser("test2@example.com", "09172222222");

        long count = bankAccountRepository.countByUser_Id(user.getId());

        assertThat(count).isEqualTo(0);
    }

    @Test
    void findByUser_Id_shouldReturnOnlyThatUsersAccounts() {

        User user = createUser("test3@example.com", "09173333333");
        User otherUser = createUser("test4@example.com", "09174444444");

        bankAccountRepository.save(new BankAccount(user, BigDecimal.valueOf(100.0), AccountStatus.ACTIVE));
        bankAccountRepository.save(new BankAccount(user, BigDecimal.valueOf(200.0), AccountStatus.ACTIVE));
        bankAccountRepository.save(new BankAccount(otherUser, BigDecimal.valueOf(300.0), AccountStatus.ACTIVE));

        Page<BankAccount> result = bankAccountRepository.findByUser_Id(user.getId(), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
            .extracting(BankAccount::getBalance)
            .containsExactlyInAnyOrder(BigDecimal.valueOf(100.0), BigDecimal.valueOf(200.0));
    }

    @Test
    void findByUser_Id_shouldReturnEmptyPage_whenUserHasNoAccounts() {

        User user = createUser("test5@example.com", "09175555555");

        Page<BankAccount> result = bankAccountRepository.findByUser_Id(user.getId(), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void save_shouldPersistBankAccount_withGeneratedIdAndDefaultBalance() {

        User user = createUser("test6@example.com", "09176666666");

        BankAccount account = new BankAccount(user, BigDecimal.ZERO, AccountStatus.ACTIVE);

        BankAccount saved = bankAccountRepository.save(account);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void findAll_shouldReturnPagedAccounts() {

        User user = createUser("test7@example.com", "09177777777");

        bankAccountRepository.save(new BankAccount(user, BigDecimal.valueOf(50.0), AccountStatus.ACTIVE));
        bankAccountRepository.save(new BankAccount(user, BigDecimal.valueOf(75.0), AccountStatus.FROZEN));

        Page<BankAccount> result = bankAccountRepository.findAll(PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }
}