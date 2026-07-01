package com.banking.system.repository;

import com.banking.system.model.entities.AppUser;
import com.banking.system.model.entities.BankAccount;
import com.banking.system.model.entities.Loan;
import com.banking.system.model.entities.LoanRepayment;
import com.banking.system.model.entities.User;
import com.banking.system.model.enums.AccountStatus;
import com.banking.system.model.enums.LoanType;
import com.banking.system.model.enums.RepaymentStatus;
import com.banking.system.model.enums.Role;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class LoanRepaymentRepositoryTests {

    @Autowired
    private LoanRepaymentRepository loanRepaymentRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppUserRepository appUserRepository;


    private Loan createLoan() {

        AppUser appUser = new AppUser();
        appUser.setEmail("user" + System.nanoTime() + "@example.com");
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

        BankAccount account = new BankAccount(user, BigDecimal.valueOf(100000), AccountStatus.ACTIVE);
        account = bankAccountRepository.save(account);

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

        return loanRepository.save(loan);
    }

    private LoanRepayment createRepayment(Loan loan, int installmentNumber, RepaymentStatus status, LocalDate dueDate) {
        LoanRepayment repayment = new LoanRepayment(loan, installmentNumber, new BigDecimal("4520.83"), dueDate);
        repayment.setStatus(status);
        return loanRepaymentRepository.save(repayment);
    }


    @Test
    void findByLoan_IdOrderByInstallmentNumberAsc_shouldReturnInOrder() {

        Loan loan = createLoan();

        createRepayment(loan, 3, RepaymentStatus.PENDING, LocalDate.now().plusMonths(3));
        createRepayment(loan, 1, RepaymentStatus.PENDING, LocalDate.now().plusMonths(1));
        createRepayment(loan, 2, RepaymentStatus.PENDING, LocalDate.now().plusMonths(2));

        List<LoanRepayment> result = loanRepaymentRepository.findByLoan_IdOrderByInstallmentNumberAsc(loan.getId());

        assertThat(result).hasSize(3);
        assertThat(result)
            .extracting(LoanRepayment::getInstallmentNumber)
            .containsExactly(1, 2, 3);
    }

    @Test
    void findByLoan_IdAndStatus_shouldReturnOnlyMatchingStatus() {

        Loan loan = createLoan();

        createRepayment(loan, 1, RepaymentStatus.PAID, LocalDate.now().minusMonths(1));
        createRepayment(loan, 2, RepaymentStatus.PENDING, LocalDate.now().plusMonths(1));
        createRepayment(loan, 3, RepaymentStatus.PENDING, LocalDate.now().plusMonths(2));

        List<LoanRepayment> result = loanRepaymentRepository.findByLoan_IdAndStatus(loan.getId(), RepaymentStatus.PENDING);

        assertThat(result).hasSize(2);
        assertThat(result)
            .extracting(LoanRepayment::getStatus)
            .containsOnly(RepaymentStatus.PENDING);
    }

    @Test
    void findFirstByLoan_IdAndStatusOrderByInstallmentNumberAsc_shouldReturnLowestInstallment() {

        Loan loan = createLoan();

        createRepayment(loan, 1, RepaymentStatus.PAID, LocalDate.now().minusMonths(1));
        createRepayment(loan, 2, RepaymentStatus.PENDING, LocalDate.now().plusMonths(1));
        createRepayment(loan, 3, RepaymentStatus.PENDING, LocalDate.now().plusMonths(2));

        Optional<LoanRepayment> result = loanRepaymentRepository
            .findFirstByLoan_IdAndStatusOrderByInstallmentNumberAsc(loan.getId(), RepaymentStatus.PENDING);

        assertThat(result).isPresent();
        assertThat(result.get().getInstallmentNumber()).isEqualTo(2);
    }

    @Test
    void findFirstByLoan_IdAndStatusOrderByInstallmentNumberAsc_shouldReturnEmpty_whenNoneMatch() {

        Loan loan = createLoan();

        createRepayment(loan, 1, RepaymentStatus.PAID, LocalDate.now().minusMonths(1));

        Optional<LoanRepayment> result = loanRepaymentRepository
            .findFirstByLoan_IdAndStatusOrderByInstallmentNumberAsc(loan.getId(), RepaymentStatus.PENDING);

        assertThat(result).isEmpty();
    }

    @Test
    void countByLoan_IdAndStatus_shouldReturnCorrectCount() {

        Loan loan = createLoan();

        createRepayment(loan, 1, RepaymentStatus.PAID, LocalDate.now().minusMonths(1));
        createRepayment(loan, 2, RepaymentStatus.PENDING, LocalDate.now().plusMonths(1));
        createRepayment(loan, 3, RepaymentStatus.PENDING, LocalDate.now().plusMonths(2));

        int count = loanRepaymentRepository.countByLoan_IdAndStatus(loan.getId(), RepaymentStatus.PENDING);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void findByStatusAndDueDateBefore_shouldReturnOverdueRepayments() {

        Loan loan = createLoan();

        createRepayment(loan, 1, RepaymentStatus.PENDING, LocalDate.now().minusDays(5));
        createRepayment(loan, 2, RepaymentStatus.PENDING, LocalDate.now().minusDays(1));
        createRepayment(loan, 3, RepaymentStatus.PENDING, LocalDate.now().plusDays(5));

        List<LoanRepayment> result = loanRepaymentRepository
            .findByStatusAndDueDateBefore(RepaymentStatus.PENDING, LocalDate.now());

        assertThat(result).hasSize(2);
        assertThat(result)
            .extracting(LoanRepayment::getInstallmentNumber)
            .containsExactlyInAnyOrder(1, 2);
    }

    @Test
    void findByStatusAndDueDateBefore_shouldNotReturn_alreadyPaidRepayments() {

        Loan loan = createLoan();

        createRepayment(loan, 1, RepaymentStatus.PAID, LocalDate.now().minusDays(5));
        createRepayment(loan, 2, RepaymentStatus.PENDING, LocalDate.now().minusDays(1));

        List<LoanRepayment> result = loanRepaymentRepository
            .findByStatusAndDueDateBefore(RepaymentStatus.PENDING, LocalDate.now());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInstallmentNumber()).isEqualTo(2);
    }

    @Test
    void save_shouldPersistRepayment_withDefaultPendingStatus() {

        Loan loan = createLoan();

        LoanRepayment repayment = new LoanRepayment(loan, 1, new BigDecimal("4520.83"), LocalDate.now().plusMonths(1));

        LoanRepayment saved = loanRepaymentRepository.save(repayment);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(RepaymentStatus.PENDING);
        assertThat(saved.getLoan().getId()).isEqualTo(loan.getId());
    }
}