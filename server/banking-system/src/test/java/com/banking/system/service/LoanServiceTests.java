package com.banking.system.service;

import com.banking.system.dto.request.LoanRequestDto;
import com.banking.system.dto.response.LoanRepaymentResponseDto;
import com.banking.system.dto.response.LoanResponseDto;
import com.banking.system.exception.AccountNotActiveException;
import com.banking.system.exception.InsufficientBalanceException;
import com.banking.system.exception.InvalidAccountStateException;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.exception.UnauthorizedActionException;
import com.banking.system.model.entities.AppUser;
import com.banking.system.model.entities.BankAccount;
import com.banking.system.model.entities.JointAccountMember;
import com.banking.system.model.entities.Loan;
import com.banking.system.model.entities.LoanRepayment;
import com.banking.system.model.entities.User;
import com.banking.system.model.enums.AccountStatus;
import com.banking.system.model.enums.LoanStatus;
import com.banking.system.model.enums.LoanType;
import com.banking.system.model.enums.RepaymentStatus;
import com.banking.system.repository.BankAccountRepository;
import com.banking.system.repository.JointAccountMemberRepository;
import com.banking.system.repository.LoanRepaymentRepository;
import com.banking.system.repository.LoanRepository;
import com.banking.system.repository.TransactionRepository;
import com.banking.system.repository.UserRepository;
import com.banking.system.services.AnalyticsService;
import com.banking.system.services.BankAccountService;
import com.banking.system.services.LoanService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class LoanServiceTests {

    @Mock private LoanRepository loanRepository;
    @Mock private LoanRepaymentRepository loanRepaymentRepository;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private UserRepository userRepository;
    @Mock private JointAccountMemberRepository jointAccountMemberRepository;
    @Mock private AnalyticsService analyticsService;
    @Mock private BankAccountService bankAccountService;

    @InjectMocks private LoanService loanService;

    private AppUser appUser;
    private User user;
    private BankAccount account;
    private Loan activeLoan;
    private Loan pendingLoan;

    @BeforeEach
    void setup() {
        appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(1L);

        user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        when(user.getAppUser()).thenReturn(appUser);

        account = mock(BankAccount.class);
        when(account.getId()).thenReturn(1L);
        when(account.getUser()).thenReturn(user);
        when(account.getBalance()).thenReturn(new BigDecimal("50000.00"));
        when(account.getStatus()).thenReturn(AccountStatus.ACTIVE);

        activeLoan = new Loan(
            user, account, LoanType.PERSONAL,
            new BigDecimal("50000.00"), new BigDecimal("0.085"), 12,
            new BigDecimal("3541.67"), new BigDecimal("4461.81"),
            new BigDecimal("53541.67"), 680
        );
        activeLoan.setStatus(LoanStatus.ACTIVE);

        pendingLoan = new Loan(
            user, account, LoanType.PERSONAL,
            new BigDecimal("50000.00"), new BigDecimal("0.085"), 12,
            new BigDecimal("3541.67"), new BigDecimal("4461.81"),
            new BigDecimal("53541.67"), 680
        );
    }

    private void mockCreditScore(int score, String recommendation) {
        Map<String, Object> loanEligibility = Map.of(
            "eligible", score >= 550,
            "recommendation", recommendation,
            "suggestedInterestRate", 8.5,
            "notes", "Test"
        );
        when(analyticsService.getCreditScore(eq(1L), eq(1L), eq(false)))
            .thenReturn(Map.of("creditScore", score, "loanEligibility", loanEligibility));
    }

    private void mockJointMember() {
        JointAccountMember member = mock(JointAccountMember.class);
        when(member.getUser()).thenReturn(user);
        when(jointAccountMemberRepository.findByBankAccount_Id(any()))
            .thenReturn(List.of(member));
    }


    // ===================== APPLY FOR LOAN =====================

    @Test
    void applyForLoan_shouldCreateLoan_whenPersonalAndEligible() {

        mockJointMember();
        mockCreditScore(680, "APPROVE");

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        doNothing().when(bankAccountService).validateActive(any());
        when(loanRepository.existsByUser_IdAndStatusIn(anyLong(), anyList())).thenReturn(false);
        when(loanRepository.save(any(Loan.class))).thenAnswer(i -> i.getArgument(0));

        LoanRequestDto request = new LoanRequestDto(1L, LoanType.PERSONAL, new BigDecimal("50000.00"), 12);
        LoanResponseDto result = loanService.applyForLoan(request, 1L);

        assertEquals(LoanType.PERSONAL, result.getType());
        assertEquals(LoanStatus.PENDING, result.getStatus());
        assertEquals(680, result.getCreditScoreAtApplication());
        verify(loanRepository, times(1)).save(any(Loan.class));
    }

    @Test
    void applyForLoan_shouldCreateBusinessLoan_whenExcellentScore() {

        mockJointMember();
        mockCreditScore(800, "AUTO_APPROVE");

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        doNothing().when(bankAccountService).validateActive(any());
        when(loanRepository.existsByUser_IdAndStatusIn(anyLong(), anyList())).thenReturn(false);
        when(loanRepository.save(any(Loan.class))).thenAnswer(i -> i.getArgument(0));

        LoanRequestDto request = new LoanRequestDto(1L, LoanType.BUSINESS, new BigDecimal("100000.00"), 24);
        LoanResponseDto result = loanService.applyForLoan(request, 1L);

        assertEquals(LoanType.BUSINESS, result.getType());
        assertEquals(LoanStatus.PENDING, result.getStatus());
    }

    @Test
    void applyForLoan_shouldThrowException_whenAccountNotFound() {

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.empty());

        LoanRequestDto request = new LoanRequestDto(1L, LoanType.PERSONAL, new BigDecimal("50000.00"), 12);

        assertThrows(ResourceNotFoundException.class, () ->
            loanService.applyForLoan(request, 1L)
        );
        verify(loanRepository, never()).save(any());
    }

    @Test
    void applyForLoan_shouldThrowException_whenAccountNotActive() {

        mockJointMember();

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        doThrow(new AccountNotActiveException(1L)).when(bankAccountService).validateActive(any());

        LoanRequestDto request = new LoanRequestDto(1L, LoanType.PERSONAL, new BigDecimal("50000.00"), 12);

        assertThrows(AccountNotActiveException.class, () ->
            loanService.applyForLoan(request, 1L)
        );
        verify(loanRepository, never()).save(any());
    }

    @Test
    void applyForLoan_shouldThrowException_whenAlreadyHasActiveLoan() {

        mockJointMember();

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        doNothing().when(bankAccountService).validateActive(any());
        when(loanRepository.existsByUser_IdAndStatusIn(anyLong(), anyList())).thenReturn(true);

        LoanRequestDto request = new LoanRequestDto(1L, LoanType.PERSONAL, new BigDecimal("50000.00"), 12);

        assertThrows(InvalidAccountStateException.class, () ->
            loanService.applyForLoan(request, 1L)
        );
        verify(loanRepository, never()).save(any());
    }

    @Test
    void applyForLoan_shouldThrowException_whenPersonalAmountBelowMinimum() {

        mockJointMember();
        mockCreditScore(680, "APPROVE");

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        doNothing().when(bankAccountService).validateActive(any());
        when(loanRepository.existsByUser_IdAndStatusIn(anyLong(), anyList())).thenReturn(false);

        LoanRequestDto request = new LoanRequestDto(1L, LoanType.PERSONAL, new BigDecimal("10000.00"), 12);

        assertThrows(InvalidAccountStateException.class, () ->
            loanService.applyForLoan(request, 1L)
        );
        verify(loanRepository, never()).save(any());
    }

    @Test
    void applyForLoan_shouldThrowException_whenBusinessAmountBelowMinimum() {

        mockJointMember();
        mockCreditScore(800, "AUTO_APPROVE");

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        doNothing().when(bankAccountService).validateActive(any());
        when(loanRepository.existsByUser_IdAndStatusIn(anyLong(), anyList())).thenReturn(false);

        LoanRequestDto request = new LoanRequestDto(1L, LoanType.BUSINESS, new BigDecimal("50000.00"), 12);

        assertThrows(InvalidAccountStateException.class, () ->
            loanService.applyForLoan(request, 1L)
        );
        verify(loanRepository, never()).save(any());
    }

    @Test
    void applyForLoan_shouldThrowException_whenCreditScoreTooLowForPersonal() {

        mockJointMember();
        mockCreditScore(400, "DENY");

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        doNothing().when(bankAccountService).validateActive(any());
        when(loanRepository.existsByUser_IdAndStatusIn(anyLong(), anyList())).thenReturn(false);

        LoanRequestDto request = new LoanRequestDto(1L, LoanType.PERSONAL, new BigDecimal("50000.00"), 12);

        assertThrows(InvalidAccountStateException.class, () ->
            loanService.applyForLoan(request, 1L)
        );
        verify(loanRepository, never()).save(any());
    }

    @Test
    void applyForLoan_shouldThrowException_whenCreditScoreTooLowForBusiness() {

        mockJointMember();
        mockCreditScore(650, "APPROVE");

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        doNothing().when(bankAccountService).validateActive(any());
        when(loanRepository.existsByUser_IdAndStatusIn(anyLong(), anyList())).thenReturn(false);

        LoanRequestDto request = new LoanRequestDto(1L, LoanType.BUSINESS, new BigDecimal("100000.00"), 12);

        assertThrows(InvalidAccountStateException.class, () ->
            loanService.applyForLoan(request, 1L)
        );
        verify(loanRepository, never()).save(any());
    }

    @Test
    void applyForLoan_shouldThrowException_whenNotAccountOwner() {

        JointAccountMember member = mock(JointAccountMember.class);
        when(member.getUser()).thenReturn(user);
        when(jointAccountMemberRepository.findByBankAccount_Id(any()))
            .thenReturn(List.of(member));

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        LoanRequestDto request = new LoanRequestDto(1L, LoanType.PERSONAL, new BigDecimal("50000.00"), 12);

        assertThrows(UnauthorizedActionException.class, () ->
            loanService.applyForLoan(request, 99L)
        );
        verify(loanRepository, never()).save(any());
    }


    // ===================== APPROVE LOAN =====================

    @Test
    void approveLoan_shouldSetStatusToApproved() {

        when(loanRepository.findById(1L)).thenReturn(Optional.of(pendingLoan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(i -> i.getArgument(0));

        LoanResponseDto result = loanService.approveLoan(1L);

        assertEquals(LoanStatus.APPROVED, result.getStatus());
        assertNotNull(result.getApprovedAt());
    }

    @Test
    void approveLoan_shouldThrowException_whenLoanAlreadyActive() {

        when(loanRepository.findById(1L)).thenReturn(Optional.of(activeLoan));

        assertThrows(InvalidAccountStateException.class, () ->
            loanService.approveLoan(1L)
        );
        verify(loanRepository, never()).save(any());
    }

    @Test
    void approveLoan_shouldThrowException_whenNotFound() {

        when(loanRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            loanService.approveLoan(1L)
        );
    }


    // ===================== REJECT LOAN =====================

    @Test
    void rejectLoan_shouldSetStatusToRejected() {

        when(loanRepository.findById(1L)).thenReturn(Optional.of(pendingLoan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(i -> i.getArgument(0));

        LoanResponseDto result = loanService.rejectLoan(1L);

        assertEquals(LoanStatus.REJECTED, result.getStatus());
    }

    @Test
    void rejectLoan_shouldThrowException_whenLoanAlreadyActive() {

        when(loanRepository.findById(1L)).thenReturn(Optional.of(activeLoan));

        assertThrows(InvalidAccountStateException.class, () ->
            loanService.rejectLoan(1L)
        );
        verify(loanRepository, never()).save(any());
    }


    // ===================== DISBURSE LOAN =====================

    @Test
    void disburseLoan_shouldCreditAccount_generateSchedule_andSetActiveStatus() {

        Loan approvedLoan = new Loan(
            user, account, LoanType.PERSONAL,
            new BigDecimal("50000.00"), new BigDecimal("0.085"), 12,
            new BigDecimal("3541.67"), new BigDecimal("4461.81"),
            new BigDecimal("53541.67"), 680
        );
        approvedLoan.setStatus(LoanStatus.APPROVED);

        when(account.getBalance()).thenReturn(new BigDecimal("50000.00"));

        when(loanRepository.findById(1L)).thenReturn(Optional.of(approvedLoan));
        doNothing().when(bankAccountService).validateActive(any());
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(loanRepaymentRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(loanRepository.save(any(Loan.class))).thenAnswer(i -> i.getArgument(0));

        LoanResponseDto result = loanService.disburseLoan(1L);

        assertEquals(LoanStatus.ACTIVE, result.getStatus());
        assertNotNull(result.getDisbursedAt());
        verify(loanRepaymentRepository, times(1)).saveAll(anyList());
        verify(transactionRepository, times(1)).save(any());
        verify(bankAccountRepository, times(1)).save(any());
    }

    @Test
    void disburseLoan_shouldThrowException_whenNotApproved() {

        when(loanRepository.findById(1L)).thenReturn(Optional.of(pendingLoan));

        assertThrows(InvalidAccountStateException.class, () ->
            loanService.disburseLoan(1L)
        );
        verify(bankAccountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
        verify(loanRepaymentRepository, never()).saveAll(any());
    }

    @Test
    void disburseLoan_shouldThrowException_whenNotFound() {

        when(loanRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            loanService.disburseLoan(1L)
        );
    }


    // ===================== MAKE REPAYMENT =====================

    @Test
    void makeRepayment_shouldDeductBalance_andMarkRepaymentPaid() {

        when(account.getBalance()).thenReturn(new BigDecimal("10000.00"));

        LoanRepayment repayment = new LoanRepayment(
            activeLoan, 1, new BigDecimal("4461.81"), LocalDate.now().plusDays(30)
        );

        when(loanRepository.findById(1L)).thenReturn(Optional.of(activeLoan));
        doNothing().when(bankAccountService).validateActive(any());
        when(loanRepaymentRepository.findFirstByLoan_IdAndStatusOrderByInstallmentNumberAsc(1L, RepaymentStatus.PENDING))
            .thenReturn(Optional.of(repayment));
        when(bankAccountRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(loanRepaymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(loanRepaymentRepository.countByLoan_IdAndStatus(1L, RepaymentStatus.PENDING)).thenReturn(5);

        LoanRepaymentResponseDto result = loanService.makeRepayment(1L, 1L);

        assertEquals(RepaymentStatus.PAID, result.getStatus());
        assertNotNull(result.getPaidAt());
        verify(transactionRepository, times(1)).save(any());
        verify(bankAccountRepository, times(1)).save(any());
    }

    @Test
    void makeRepayment_shouldMarkLoanAsPaid_whenLastInstallment() {

        when(account.getBalance()).thenReturn(new BigDecimal("10000.00"));

        LoanRepayment lastRepayment = new LoanRepayment(
            activeLoan, 12, new BigDecimal("4461.81"), LocalDate.now().plusDays(30)
        );

        when(loanRepository.findById(1L)).thenReturn(Optional.of(activeLoan));
        doNothing().when(bankAccountService).validateActive(any());
        when(loanRepaymentRepository.findFirstByLoan_IdAndStatusOrderByInstallmentNumberAsc(1L, RepaymentStatus.PENDING))
            .thenReturn(Optional.of(lastRepayment));
        when(bankAccountRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(loanRepaymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(loanRepaymentRepository.countByLoan_IdAndStatus(1L, RepaymentStatus.PENDING)).thenReturn(0);
        when(loanRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        loanService.makeRepayment(1L, 1L);

        verify(loanRepository, times(1)).save(any(Loan.class));
        assertEquals(LoanStatus.PAID, activeLoan.getStatus());
    }

    @Test
    void makeRepayment_shouldThrowException_whenInsufficientBalance() {

        when(account.getBalance()).thenReturn(new BigDecimal("100.00"));

        LoanRepayment repayment = new LoanRepayment(
            activeLoan, 1, new BigDecimal("4461.81"), LocalDate.now().plusDays(30)
        );

        when(loanRepository.findById(1L)).thenReturn(Optional.of(activeLoan));
        doNothing().when(bankAccountService).validateActive(any());
        when(loanRepaymentRepository.findFirstByLoan_IdAndStatusOrderByInstallmentNumberAsc(1L, RepaymentStatus.PENDING))
            .thenReturn(Optional.of(repayment));

        assertThrows(InsufficientBalanceException.class, () ->
            loanService.makeRepayment(1L, 1L)
        );
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void makeRepayment_shouldThrowException_whenNotOwner() {

        when(loanRepository.findById(1L)).thenReturn(Optional.of(activeLoan));

        assertThrows(UnauthorizedActionException.class, () ->
            loanService.makeRepayment(1L, 99L)
        );
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void makeRepayment_shouldThrowException_whenLoanNotActive() {

        when(loanRepository.findById(1L)).thenReturn(Optional.of(pendingLoan));

        assertThrows(InvalidAccountStateException.class, () ->
            loanService.makeRepayment(1L, 1L)
        );
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void makeRepayment_shouldThrowException_whenNoPendingRepayments() {

        when(account.getBalance()).thenReturn(new BigDecimal("10000.00"));

        when(loanRepository.findById(1L)).thenReturn(Optional.of(activeLoan));
        doNothing().when(bankAccountService).validateActive(any());
        when(loanRepaymentRepository.findFirstByLoan_IdAndStatusOrderByInstallmentNumberAsc(1L, RepaymentStatus.PENDING))
            .thenReturn(Optional.empty());

        assertThrows(InvalidAccountStateException.class, () ->
            loanService.makeRepayment(1L, 1L)
        );
    }


    // ===================== MARK OVERDUE REPAYMENTS =====================

    @Test
    void markOverdueRepayments_shouldMarkRepaymentAsOverdue() {

        LoanRepayment overdue = new LoanRepayment(
            activeLoan, 1, new BigDecimal("4461.81"), LocalDate.now().minusDays(5)
        );

        when(loanRepaymentRepository.findByStatusAndDueDateBefore(eq(RepaymentStatus.PENDING), any()))
            .thenReturn(List.of(overdue));
        when(loanRepaymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(loanRepaymentRepository.countByLoan_IdAndStatus(any(), eq(RepaymentStatus.OVERDUE))).thenReturn(1);

        loanService.markOverdueRepayments();

        assertEquals(RepaymentStatus.OVERDUE, overdue.getStatus());
        verify(loanRepaymentRepository, times(1)).save(overdue);
    }

    @Test
    void markOverdueRepayments_shouldDefaultLoan_andFreezeAccount_whenThreeConsecutiveOverdue() {

        LoanRepayment overdue = new LoanRepayment(
            activeLoan, 3, new BigDecimal("4461.81"), LocalDate.now().minusDays(5)
        );

        when(account.getStatus()).thenReturn(AccountStatus.ACTIVE);

        when(loanRepaymentRepository.findByStatusAndDueDateBefore(eq(RepaymentStatus.PENDING), any()))
            .thenReturn(List.of(overdue));
        when(loanRepaymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(loanRepaymentRepository.countByLoan_IdAndStatus(any(), eq(RepaymentStatus.OVERDUE))).thenReturn(3);
        when(loanRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bankAccountRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        loanService.markOverdueRepayments();

        assertEquals(LoanStatus.DEFAULTED, activeLoan.getStatus());
        verify(loanRepository, times(1)).save(activeLoan);
        verify(bankAccountRepository, times(1)).save(account);
    }

    @Test
    void markOverdueRepayments_shouldNotFreezeAccount_whenAlreadyFrozen() {

        when(account.getStatus()).thenReturn(AccountStatus.FROZEN);

        LoanRepayment overdue = new LoanRepayment(
            activeLoan, 3, new BigDecimal("4461.81"), LocalDate.now().minusDays(5)
        );

        when(loanRepaymentRepository.findByStatusAndDueDateBefore(eq(RepaymentStatus.PENDING), any()))
            .thenReturn(List.of(overdue));
        when(loanRepaymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(loanRepaymentRepository.countByLoan_IdAndStatus(any(), eq(RepaymentStatus.OVERDUE))).thenReturn(3);
        when(loanRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        loanService.markOverdueRepayments();

        assertEquals(LoanStatus.DEFAULTED, activeLoan.getStatus());
        verify(bankAccountRepository, never()).save(any());
    }


    // ===================== GET BY ID =====================

    @Test
    void getById_shouldReturnLoan_whenOwner() {

        when(loanRepository.findById(1L)).thenReturn(Optional.of(activeLoan));

        LoanResponseDto result = loanService.getById(1L, 1L, false);

        assertEquals(LoanType.PERSONAL, result.getType());
        assertEquals(LoanStatus.ACTIVE, result.getStatus());
    }

    @Test
    void getById_shouldReturnLoan_whenAdmin() {

        when(loanRepository.findById(1L)).thenReturn(Optional.of(activeLoan));

        LoanResponseDto result = loanService.getById(1L, 999L, true);

        assertNotNull(result);
    }

    @Test
    void getById_shouldThrowException_whenNotOwner() {

        when(loanRepository.findById(1L)).thenReturn(Optional.of(activeLoan));

        assertThrows(UnauthorizedActionException.class, () ->
            loanService.getById(1L, 99L, false)
        );
    }

    @Test
    void getById_shouldThrowException_whenNotFound() {

        when(loanRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            loanService.getById(1L, 1L, false)
        );
    }


    // ===================== GET BY USER ID =====================

    @Test
    void getByUserId_shouldReturnLoans_whenOwner() {

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(loanRepository.findByUser_Id(eq(10L), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(activeLoan)));

        Page<LoanResponseDto> result = loanService.getByUserId(10L, 1L, false, Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getByUserId_shouldThrowException_whenNotOwner() {

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedActionException.class, () ->
            loanService.getByUserId(10L, 99L, false, Pageable.unpaged())
        );
    }


    // ===================== GET REPAYMENT SCHEDULE =====================

    @Test
    void getRepaymentSchedule_shouldReturnSchedule_whenOwner() {

        LoanRepayment repayment = new LoanRepayment(
            activeLoan, 1, new BigDecimal("4461.81"), LocalDate.now().plusMonths(1)
        );

        when(loanRepository.findById(1L)).thenReturn(Optional.of(activeLoan));
        when(loanRepaymentRepository.findByLoan_IdOrderByInstallmentNumberAsc(1L))
            .thenReturn(List.of(repayment));

        List<LoanRepaymentResponseDto> result = loanService.getRepaymentSchedule(1L, 1L, false);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getInstallmentNumber());
        assertEquals(RepaymentStatus.PENDING, result.get(0).getStatus());
    }

    @Test
    void getRepaymentSchedule_shouldThrowException_whenNotOwner() {

        when(loanRepository.findById(1L)).thenReturn(Optional.of(activeLoan));

        assertThrows(UnauthorizedActionException.class, () ->
            loanService.getRepaymentSchedule(1L, 99L, false)
        );
    }
}