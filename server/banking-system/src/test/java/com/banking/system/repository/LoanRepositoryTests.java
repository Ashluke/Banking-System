package com.banking.system.repository;

import com.banking.system.model.entities.AppUser;
import com.banking.system.model.entities.BankAccount;
import com.banking.system.model.entities.Loan;
import com.banking.system.model.entities.User;
import com.banking.system.model.enums.AccountStatus;
import com.banking.system.model.enums.LoanStatus;
import com.banking.system.model.enums.LoanType;
import com.banking.system.model.enums.Role;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class LoanRepositoryTests {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppUserRepository appUserRepository;


    private User createUser(String email) {

        AppUser appUser = new AppUser();
        appUser.setEmail(email);
        appUser.setPasswordHash("hashed");
        appUser.setRole(Role.USER);
        appUser = appUserRepository.save(appUser);

        User user = new User();
        user.setAppUser(appUser);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPhoneNumber("0917" + System.nanoTime() % 1000000000L);
        user.setAddress("123 Main St");

        return userRepository.save(user);
    }

    private BankAccount createAccount(User user) {
        BankAccount account = new BankAccount(user, BigDecimal.valueOf(100000), AccountStatus.ACTIVE);
        return bankAccountRepository.save(account);
    }

    private Loan createLoan(User user, BankAccount account, LoanStatus status) {
        Loan loan = new Loan(
            user, account, LoanType.PERSONAL,
            new BigDecimal("50000.00"),
            new BigDecimal("0.085"),
            12,
            new BigDecimal("4250.00"),
            new BigDecimal("4520.83"),
            new BigDecimal("54250.00"),
            680
        );
        loan.setStatus(status);
        return loanRepository.save(loan);
    }


    @Test
    void findByUser_Id_shouldReturnLoans_forThatUser() {

        User user = createUser("user1@example.com");
        User otherUser = createUser("user2@example.com");
        BankAccount account = createAccount(user);
        BankAccount otherAccount = createAccount(otherUser);

        createLoan(user, account, LoanStatus.PENDING);
        createLoan(user, account, LoanStatus.ACTIVE);
        createLoan(otherUser, otherAccount, LoanStatus.PENDING);

        Page<Loan> result = loanRepository.findByUser_Id(user.getId(), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
            .extracting(Loan::getUser)
            .allMatch(u -> u.getId().equals(user.getId()));
    }

    @Test
    void findByUser_Id_shouldReturnEmptyPage_whenUserHasNoLoans() {

        User user = createUser("user3@example.com");

        Page<Loan> result = loanRepository.findByUser_Id(user.getId(), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void findByStatus_paged_shouldReturnOnlyMatchingLoans() {

        User user = createUser("user4@example.com");
        BankAccount account = createAccount(user);

        createLoan(user, account, LoanStatus.PENDING);
        createLoan(user, account, LoanStatus.PENDING);
        createLoan(user, account, LoanStatus.ACTIVE);

        Page<Loan> result = loanRepository.findByStatus(LoanStatus.PENDING, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
            .extracting(Loan::getStatus)
            .containsOnly(LoanStatus.PENDING);
    }

    @Test
    void findByStatus_list_shouldReturnOnlyMatchingLoans() {

        User user = createUser("user5@example.com");
        BankAccount account = createAccount(user);

        createLoan(user, account, LoanStatus.ACTIVE);
        createLoan(user, account, LoanStatus.PAID);
        createLoan(user, account, LoanStatus.PENDING);

        List<Loan> result = loanRepository.findByStatus(LoanStatus.ACTIVE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(LoanStatus.ACTIVE);
    }

    @Test
    void existsByUser_IdAndStatusIn_shouldReturnTrue_whenMatchingLoanExists() {

        User user = createUser("user6@example.com");
        BankAccount account = createAccount(user);

        createLoan(user, account, LoanStatus.ACTIVE);

        boolean exists = loanRepository.existsByUser_IdAndStatusIn(
            user.getId(),
            List.of(LoanStatus.PENDING, LoanStatus.ACTIVE)
        );

        assertThat(exists).isTrue();
    }

    @Test
    void existsByUser_IdAndStatusIn_shouldReturnFalse_whenNoMatchingLoan() {

        User user = createUser("user7@example.com");
        BankAccount account = createAccount(user);

        createLoan(user, account, LoanStatus.PAID);

        boolean exists = loanRepository.existsByUser_IdAndStatusIn(
            user.getId(),
            List.of(LoanStatus.PENDING, LoanStatus.ACTIVE)
        );

        assertThat(exists).isFalse();
    }

    @Test
    void save_shouldPersistLoan_withDefaultPendingStatus() {

        User user = createUser("user8@example.com");
        BankAccount account = createAccount(user);

        Loan loan = new Loan(
            user, account, LoanType.PERSONAL,
            new BigDecimal("50000.00"),
            new BigDecimal("0.085"),
            12,
            new BigDecimal("4250.00"),
            new BigDecimal("4520.83"),
            new BigDecimal("54250.00"),
            680
        );

        Loan saved = loanRepository.save(loan);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(LoanStatus.PENDING);
        assertThat(saved.getAppliedAt()).isNotNull();
    }
}