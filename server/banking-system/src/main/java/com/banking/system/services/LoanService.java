package com.banking.system.services;

import com.banking.system.dto.request.LoanRequestDto;
import com.banking.system.dto.response.LoanRepaymentResponseDto;
import com.banking.system.dto.response.LoanResponseDto;
import com.banking.system.exception.InvalidAccountStateException;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.exception.UnauthorizedActionException;
import com.banking.system.model.entities.BankAccount;
import com.banking.system.model.entities.Loan;
import com.banking.system.model.entities.LoanRepayment;
import com.banking.system.model.entities.Transaction;
import com.banking.system.model.entities.User;
import com.banking.system.model.enums.AccountStatus;
import com.banking.system.model.enums.LoanStatus;
import com.banking.system.model.enums.LoanType;
import com.banking.system.model.enums.RepaymentStatus;
import com.banking.system.model.enums.TransactionType;
import com.banking.system.repository.BankAccountRepository;
import com.banking.system.repository.JointAccountMemberRepository;
import com.banking.system.repository.LoanRepaymentRepository;
import com.banking.system.repository.LoanRepository;
import com.banking.system.repository.TransactionRepository;
import com.banking.system.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class LoanService {

    // Credit score thresholds
    private static final int PERSONAL_MIN_SCORE   = 550;
    private static final int BUSINESS_MIN_SCORE   = 750;

    // Minimum loan amounts
    private static final BigDecimal PERSONAL_MIN_AMOUNT = new BigDecimal("40000.00");
    private static final BigDecimal BUSINESS_MIN_AMOUNT = new BigDecimal("100000.00");

    // Default interest rates
    private static final BigDecimal PERSONAL_RATE_EXCELLENT = new BigDecimal("0.05");
    private static final BigDecimal PERSONAL_RATE_GOOD      = new BigDecimal("0.085");
    private static final BigDecimal PERSONAL_RATE_FAIR      = new BigDecimal("0.12");
    private static final BigDecimal BUSINESS_RATE           = new BigDecimal("0.07");

    private final LoanRepository loanRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final JointAccountMemberRepository jointAccountMemberRepository;
    private final AnalyticsService analyticsService;
    private final BankAccountService bankAccountService;

    public LoanService(
            LoanRepository loanRepository,
            LoanRepaymentRepository loanRepaymentRepository,
            BankAccountRepository bankAccountRepository,
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            JointAccountMemberRepository jointAccountMemberRepository,
            AnalyticsService analyticsService,
            BankAccountService bankAccountService) {
        this.loanRepository = loanRepository;
        this.loanRepaymentRepository = loanRepaymentRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.jointAccountMemberRepository = jointAccountMemberRepository;
        this.analyticsService = analyticsService;
        this.bankAccountService = bankAccountService;
    }

    // Apply for loan

    @Transactional
    public LoanResponseDto applyForLoan(LoanRequestDto request, Long appUserId) {

        BankAccount account = bankAccountRepository.findById(request.getBankAccountId()).orElseThrow(() ->
            new ResourceNotFoundException("Account not found"));

        checkOwnershipOrAdmin(account, appUserId, false);
        bankAccountService.validateActive(account);

        User user = account.getUser();

        // Block if user already has an active or pending loan
        if (loanRepository.existsByUser_IdAndStatusIn(user.getId(),
                List.of(LoanStatus.PENDING, LoanStatus.UNDER_REVIEW, LoanStatus.APPROVED, LoanStatus.ACTIVE))) {
            throw new InvalidAccountStateException("You already have an active or pending loan");
        }

        // Get credit score from Python analytics
        Map<String, Object> creditScoreResult = analyticsService.getCreditScore(
            account.getId(), appUserId, false
        );

        int creditScore = ((Number) creditScoreResult.get("creditScore")).intValue();
        String recommendation = (String) ((Map<?, ?>) creditScoreResult.get("loanEligibility")).get("recommendation");

        // Validate loan type eligibility
        validateLoanEligibility(request.getType(), request.getAmount(), creditScore, recommendation);

        // Calculate interest and payments
        BigDecimal interestRate = resolveInterestRate(request.getType(), creditScore);
        BigDecimal totalInterest = request.getAmount()
            .multiply(interestRate)
            .multiply(BigDecimal.valueOf(request.getTermMonths()))
            .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        BigDecimal totalAmount = request.getAmount().add(totalInterest);
        BigDecimal monthlyPayment = totalAmount
            .divide(BigDecimal.valueOf(request.getTermMonths()), 2, RoundingMode.HALF_UP);

        Loan loan = new Loan(
            user,
            account,
            request.getType(),
            request.getAmount(),
            interestRate,
            request.getTermMonths(),
            totalInterest,
            monthlyPayment,
            totalAmount,
            creditScore
        );

        Loan saved = loanRepository.save(loan);

        return mapToResponse(saved);
    }

    // Approve loan

    @Transactional
    public LoanResponseDto approveLoan(Long loanId) {

        Loan loan = loanRepository.findById(loanId).orElseThrow(() ->
            new ResourceNotFoundException("Loan not found"));

        if (loan.getStatus() != LoanStatus.PENDING && loan.getStatus() != LoanStatus.UNDER_REVIEW) {
            throw new InvalidAccountStateException("Only PENDING or UNDER_REVIEW loans can be approved");
        }

        loan.setStatus(LoanStatus.APPROVED);
        loan.setApprovedAt(LocalDateTime.now());

        return mapToResponse(loanRepository.save(loan));
    }

    // Reject loan

    @Transactional
    public LoanResponseDto rejectLoan(Long loanId) {

        Loan loan = loanRepository.findById(loanId).orElseThrow(() ->
            new ResourceNotFoundException("Loan not found"));

        if (loan.getStatus() != LoanStatus.PENDING && loan.getStatus() != LoanStatus.UNDER_REVIEW) {
            throw new InvalidAccountStateException("Only PENDING or UNDER_REVIEW loans can be rejected");
        }

        loan.setStatus(LoanStatus.REJECTED);

        return mapToResponse(loanRepository.save(loan));
    }

    // Disburse loan

    @Transactional
    public LoanResponseDto disburseLoan(Long loanId) {

        Loan loan = loanRepository.findById(loanId).orElseThrow(() ->
            new ResourceNotFoundException("Loan not found"));

        if (loan.getStatus() != LoanStatus.APPROVED) {
            throw new InvalidAccountStateException("Only APPROVED loans can be disbursed");
        }

        BankAccount account = loan.getBankAccount();
        bankAccountService.validateActive(account);

        // Credit loan amount to account as TRANSFER_IN
        account.setBalance(account.getBalance().add(loan.getAmount()));
        bankAccountRepository.save(account);

        Transaction disbursementTx = new Transaction(
            account,
            loan.getAmount(),
            TransactionType.TRANSFER_IN
        );
        transactionRepository.save(disbursementTx);

        // Generate repayment schedule
        List<LoanRepayment> repayments = new ArrayList<>();
        LocalDate startDate = LocalDate.now().plusMonths(1);
        for (int i = 1; i <= loan.getTermMonths(); i++) {
            repayments.add(new LoanRepayment(
                loan,
                i,
                loan.getMonthlyPayment(),
                startDate.plusMonths(i - 1)
            ));
        }
        loanRepaymentRepository.saveAll(repayments);

        loan.setStatus(LoanStatus.ACTIVE);
        loan.setDisbursedAt(LocalDateTime.now());

        return mapToResponse(loanRepository.save(loan));
    }

    // Make repayment

    @Transactional
    public LoanRepaymentResponseDto makeRepayment(Long loanId, Long appUserId) {

        Loan loan = loanRepository.findById(loanId).orElseThrow(() ->
            new ResourceNotFoundException("Loan not found"));

        if (!loan.getUser().getAppUser().getId().equals(appUserId)) {
            throw new UnauthorizedActionException("You do not own this loan");
        }

        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new InvalidAccountStateException("Only ACTIVE loans can be repaid");
        }

        // Get next unpaid installment
        LoanRepayment repayment = loanRepaymentRepository
            .findFirstByLoan_IdAndStatusOrderByInstallmentNumberAsc(loanId, RepaymentStatus.PENDING)
            .orElseThrow(() -> new InvalidAccountStateException("No pending repayments found"));

        BankAccount account = loan.getBankAccount();
        bankAccountService.validateActive(account);

        if (account.getBalance().compareTo(repayment.getAmount()) < 0) {
            throw new com.banking.system.exception.InsufficientBalanceException("Insufficient balance for repayment");
        }

        // Deduct from account as WITHDRAW
        account.setBalance(account.getBalance().subtract(repayment.getAmount()));
        bankAccountRepository.save(account);

        Transaction repaymentTx = new Transaction(
            account,
            repayment.getAmount(),
            TransactionType.WITHDRAW
        );
        transactionRepository.save(repaymentTx);

        // Mark repayment as paid
        repayment.setStatus(RepaymentStatus.PAID);
        repayment.setPaidAt(LocalDateTime.now());
        loanRepaymentRepository.save(repayment);

        // Check if all repayments are done
        int remainingPending = loanRepaymentRepository.countByLoan_IdAndStatus(loanId, RepaymentStatus.PENDING);
        if (remainingPending == 0) {
            loan.setStatus(LoanStatus.PAID);
            loanRepository.save(loan);
        }

        return mapRepaymentToResponse(repayment);
    }

    // Get loan by id

    public LoanResponseDto getById(Long loanId, Long appUserId, boolean isAdmin) {

        Loan loan = loanRepository.findById(loanId).orElseThrow(() ->
            new ResourceNotFoundException("Loan not found"));

        if (!isAdmin && !loan.getUser().getAppUser().getId().equals(appUserId)) {
            throw new UnauthorizedActionException("You do not own this loan");
        }

        return mapToResponse(loan);
    }

    // Get loans by user

    public Page<LoanResponseDto> getByUserId(Long userId, Long appUserId, boolean isAdmin, Pageable pageable) {

        User user = userRepository.findById(userId).orElseThrow(() ->
            new ResourceNotFoundException("User not found"));

        if (!isAdmin && !user.getAppUser().getId().equals(appUserId)) {
            throw new UnauthorizedActionException("You do not have access to these loans");
        }

        return loanRepository.findByUser_Id(userId, pageable).map(this::mapToResponse);
    }

    // Get all loasns

    public Page<LoanResponseDto> getAll(Pageable pageable) {
        return loanRepository.findAll(pageable).map(this::mapToResponse);
    }

    // Get repayment schedule

    public List<LoanRepaymentResponseDto> getRepaymentSchedule(Long loanId, Long appUserId, boolean isAdmin) {

        Loan loan = loanRepository.findById(loanId).orElseThrow(() ->
            new ResourceNotFoundException("Loan not found"));

        if (!isAdmin && !loan.getUser().getAppUser().getId().equals(appUserId)) {
            throw new UnauthorizedActionException("You do not own this loan");
        }

        return loanRepaymentRepository.findByLoan_IdOrderByInstallmentNumberAsc(loanId)
            .stream()
            .map(this::mapRepaymentToResponse)
            .toList();
    }

    // Mark overdue payments

    @Transactional
    public void markOverdueRepayments() {

        LocalDate today = LocalDate.now();

        List<LoanRepayment> overdueRepayments = loanRepaymentRepository
            .findByStatusAndDueDateBefore(RepaymentStatus.PENDING, today);

        for (LoanRepayment repayment : overdueRepayments) {
            repayment.setStatus(RepaymentStatus.OVERDUE);
            loanRepaymentRepository.save(repayment);

            // Check consecutive overdue count
            int overdueCount = loanRepaymentRepository
                .countByLoan_IdAndStatus(repayment.getLoan().getId(), RepaymentStatus.OVERDUE);

            if (overdueCount >= 3) {
                Loan loan = repayment.getLoan();
                loan.setStatus(LoanStatus.DEFAULTED);
                loanRepository.save(loan);

                // Freeze the account
                BankAccount account = loan.getBankAccount();
                if (account.getStatus() == AccountStatus.ACTIVE) {
                    account.setStatus(AccountStatus.FROZEN);
                    bankAccountRepository.save(account);
                }
            }
        }
    }

    // Helpers

    private void validateLoanEligibility(LoanType type, BigDecimal amount, int creditScore, String recommendation) {

        if (type == LoanType.PERSONAL) {
            if (amount.compareTo(PERSONAL_MIN_AMOUNT) < 0) {
                throw new InvalidAccountStateException(
                    "Minimum personal loan amount is ₱40,000.00"
                );
            }
            if (creditScore < PERSONAL_MIN_SCORE) {
                throw new InvalidAccountStateException(
                    "Credit score of " + creditScore + " is below the minimum required (" + PERSONAL_MIN_SCORE + ") for a personal loan"
                );
            }
        }

        if (type == LoanType.BUSINESS) {
            if (amount.compareTo(BUSINESS_MIN_AMOUNT) < 0) {
                throw new InvalidAccountStateException(
                    "Minimum business loan amount is ₱100,000.00"
                );
            }
            if (creditScore < BUSINESS_MIN_SCORE) {
                throw new InvalidAccountStateException(
                    "Business loans require an EXCELLENT credit score (" + BUSINESS_MIN_SCORE + "+). Current score: " + creditScore
                );
            }
            if ("DENY".equals(recommendation) || "MANUAL_REVIEW".equals(recommendation)) {
                throw new InvalidAccountStateException(
                    "Your credit profile does not qualify for a business loan at this time"
                );
            }
        }

        if ("DENY".equals(recommendation)) {
            throw new InvalidAccountStateException(
                "Loan application denied based on credit score assessment"
            );
        }
    }

    private BigDecimal resolveInterestRate(LoanType type, int creditScore) {
        if (type == LoanType.BUSINESS) return BUSINESS_RATE;
        if (creditScore >= 750) return PERSONAL_RATE_EXCELLENT;
        if (creditScore >= 650) return PERSONAL_RATE_GOOD;
        return PERSONAL_RATE_FAIR;
    }

    private void checkOwnershipOrAdmin(BankAccount account, Long appUserId, boolean isAdmin) {

        if (isAdmin) return;

        boolean isMember = jointAccountMemberRepository
            .findByBankAccount_Id(account.getId())
            .stream()
            .anyMatch(m -> m.getUser().getAppUser().getId().equals(appUserId));

        if (!isMember) {
            throw new UnauthorizedActionException("You do not have access to this account");
        }
    }

    private LoanResponseDto mapToResponse(Loan loan) {
        return new LoanResponseDto(
            loan.getId(),
            loan.getUser().getId(),
            loan.getBankAccount().getId(),
            loan.getType(),
            loan.getStatus(),
            loan.getAmount(),
            loan.getInterestRate(),
            loan.getTermMonths(),
            loan.getTotalInterest(),
            loan.getMonthlyPayment(),
            loan.getTotalAmount(),
            loan.getCreditScoreAtApplication(),
            loan.getApprovedAt(),
            loan.getDisbursedAt(),
            loan.getAppliedAt()
        );
    }

    private LoanRepaymentResponseDto mapRepaymentToResponse(LoanRepayment repayment) {
        return new LoanRepaymentResponseDto(
            repayment.getId(),
            repayment.getLoan().getId(),
            repayment.getInstallmentNumber(),
            repayment.getAmount(),
            repayment.getDueDate(),
            repayment.getPaidAt(),
            repayment.getStatus()
        );
    }
}