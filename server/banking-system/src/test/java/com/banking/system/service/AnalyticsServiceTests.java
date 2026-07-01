package com.banking.system.service;

import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.exception.UnauthorizedActionException;
import com.banking.system.model.entities.AppUser;
import com.banking.system.model.entities.BankAccount;
import com.banking.system.model.entities.JointAccountMember;
import com.banking.system.model.entities.StockHolding;
import com.banking.system.model.entities.User;
import com.banking.system.model.enums.AccountStatus;
import com.banking.system.dto.response.StockQuoteResponseDto;
import com.banking.system.repository.BankAccountRepository;
import com.banking.system.repository.JointAccountMemberRepository;
import com.banking.system.repository.StockHoldingRepository;
import com.banking.system.repository.TransactionRepository;
import com.banking.system.repository.UserRepository;
import com.banking.system.services.AnalyticsService;
import com.banking.system.services.StockService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AnalyticsServiceTests {

    @Mock private RestClient restClient;
    @Mock private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock private RestClient.RequestBodySpec requestBodySpec;
    @Mock private RestClient.ResponseSpec responseSpec;

    @Mock private UserRepository userRepository;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private StockHoldingRepository stockHoldingRepository;
    @Mock private JointAccountMemberRepository jointAccountMemberRepository;
    @Mock private StockService stockService;

    @InjectMocks private AnalyticsService analyticsService;

    private AppUser appUser;
    private User user;
    private BankAccount account;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(analyticsService, "analyticsServiceUrl", "http://localhost:8000");

        appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(1L);
        when(appUser.getCreatedAt()).thenReturn(LocalDateTime.now().minusYears(1));

        user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        when(user.getAppUser()).thenReturn(appUser);

        account = mock(BankAccount.class);
        when(account.getId()).thenReturn(1L);
        when(account.getUser()).thenReturn(user);
        when(account.getBalance()).thenReturn(new BigDecimal("5000.00"));
        when(account.getStatus()).thenReturn(AccountStatus.ACTIVE);
    }

    private void mockPythonResponse(Map<String, Object> response) {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Map.class)).thenReturn(response);
    }

    private void mockJointMember() {
        JointAccountMember member = mock(JointAccountMember.class);
        when(member.getUser()).thenReturn(user);
        when(jointAccountMemberRepository.findByBankAccount_Id(any()))
            .thenReturn(List.of(member));
    }


    // ===================== GET TRANSACTION INSIGHTS =====================

    @Test
    void getTransactionInsights_shouldCallPython_whenOwner() {

        mockJointMember();
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(transactionRepository.findByBankAccount_Id(eq(1L), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));
        mockPythonResponse(Map.of("summary", Map.of("totalTransactions", 0)));

        Map<String, Object> result = analyticsService.getTransactionInsights(1L, 1L, false);

        assertNotNull(result);
        verify(restClient, times(1)).post();
    }

    @Test
    void getTransactionInsights_shouldThrowException_whenAccountNotFound() {

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            analyticsService.getTransactionInsights(1L, 1L, false)
        );
        verifyNoInteractions(restClient);
    }

    @Test
    void getTransactionInsights_shouldThrowException_whenNotOwner() {

        mockJointMember();
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThrows(UnauthorizedActionException.class, () ->
            analyticsService.getTransactionInsights(1L, 99L, false)
        );
        verifyNoInteractions(restClient);
    }

    @Test
    void getTransactionInsights_shouldBypassOwnershipCheck_whenAdmin() {

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(transactionRepository.findByBankAccount_Id(eq(1L), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));
        mockPythonResponse(Map.of("summary", Map.of("totalTransactions", 0)));

        Map<String, Object> result = analyticsService.getTransactionInsights(1L, 999L, true);

        assertNotNull(result);
        verify(jointAccountMemberRepository, never()).findByBankAccount_Id(any());
    }


    // ===================== GET SPENDING TRENDS =====================

    @Test
    void getSpendingTrends_shouldCallPython_whenOwner() {

        mockJointMember();
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(transactionRepository.findByBankAccount_Id(eq(1L), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));
        mockPythonResponse(Map.of("spendingTrend", "INSUFFICIENT_DATA"));

        Map<String, Object> result = analyticsService.getSpendingTrends(1L, 1L, false);

        assertNotNull(result);
        verify(restClient, times(1)).post();
    }

    @Test
    void getSpendingTrends_shouldThrowException_whenNotOwner() {

        mockJointMember();
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThrows(UnauthorizedActionException.class, () ->
            analyticsService.getSpendingTrends(1L, 99L, false)
        );
        verifyNoInteractions(restClient);
    }


    // ===================== DETECT FRAUD =====================

    @Test
    void detectFraud_shouldCallPython_whenOwner() {

        mockJointMember();
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(transactionRepository.findByBankAccount_Id(eq(1L), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));
        mockPythonResponse(Map.of("riskLevel", "LOW"));

        Map<String, Object> result = analyticsService.detectFraud(1L, 1L, false);

        assertNotNull(result);
        verify(restClient, times(1)).post();
    }

    @Test
    void detectFraud_shouldThrowException_whenAccountNotFound() {

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            analyticsService.detectFraud(1L, 1L, false)
        );
    }


    // ===================== PREDICT SAVINGS =====================

    @Test
    void predictSavings_shouldCallPython_whenOwner() {

        mockJointMember();
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(transactionRepository.findByBankAccount_Id(eq(1L), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));
        mockPythonResponse(Map.of("savingsTrend", "INSUFFICIENT_DATA"));

        Map<String, Object> result = analyticsService.predictSavings(1L, 1L, false);

        assertNotNull(result);
        verify(restClient, times(1)).post();
    }

    @Test
    void predictSavings_shouldThrowException_whenNotOwner() {

        mockJointMember();
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThrows(UnauthorizedActionException.class, () ->
            analyticsService.predictSavings(1L, 99L, false)
        );
    }


    // ===================== GET CREDIT SCORE =====================

    @Test
    void getCreditScore_shouldCallPython_withTransactionAndAccountData() {

        mockJointMember();
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(transactionRepository.findByBankAccount_Id(eq(1L), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));
        mockPythonResponse(Map.of("creditScore", 680, "rating", "GOOD"));

        Map<String, Object> result = analyticsService.getCreditScore(1L, 1L, false);

        assertNotNull(result);
        assertEquals(680, result.get("creditScore"));
        verify(restClient, times(1)).post();
    }

    @Test
    void getCreditScore_shouldThrowException_whenAccountNotFound() {

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            analyticsService.getCreditScore(1L, 1L, false)
        );
    }

    @Test
    void getCreditScore_shouldThrowException_whenNotOwner() {

        mockJointMember();
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThrows(UnauthorizedActionException.class, () ->
            analyticsService.getCreditScore(1L, 99L, false)
        );
        verifyNoInteractions(restClient);
    }


    // ===================== GET PORTFOLIO ANALYTICS =====================

    @Test
    void getPortfolioAnalytics_shouldReturnEmptyResult_whenNoHoldings() {

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(stockHoldingRepository.findByUser_Id(10L)).thenReturn(List.of());

        Map<String, Object> result = analyticsService.getPortfolioAnalytics(10L, 1L, false);

        assertNotNull(result);
        Map<?, ?> summary = (Map<?, ?>) result.get("summary");
        assertEquals(0, summary.get("numberOfHoldings"));
        verifyNoInteractions(restClient);
    }

    @Test
    void getPortfolioAnalytics_shouldFetchQuotes_andCallPython_whenHoldingsExist() {

        StockHolding holding = new StockHolding(user, "AAPL", new BigDecimal("10"), new BigDecimal("100.00"));

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(stockHoldingRepository.findByUser_Id(10L)).thenReturn(List.of(holding));

        StockQuoteResponseDto quote = new StockQuoteResponseDto(
            "AAPL", 150.0, 5.0, 3.4, 155.0, 145.0, 148.0, 145.0
        );
        when(stockService.fetchQuotes(any())).thenReturn(List.of(quote));

        mockPythonResponse(Map.of("summary", Map.of("numberOfHoldings", 1)));

        Map<String, Object> result = analyticsService.getPortfolioAnalytics(10L, 1L, false);

        assertNotNull(result);
        verify(stockService, times(1)).fetchQuotes(any());
        verify(restClient, times(1)).post();
    }

    @Test
    void getPortfolioAnalytics_shouldThrowException_whenUserNotFound() {

        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            analyticsService.getPortfolioAnalytics(10L, 1L, false)
        );
    }

    @Test
    void getPortfolioAnalytics_shouldThrowException_whenNotOwner() {

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedActionException.class, () ->
            analyticsService.getPortfolioAnalytics(10L, 99L, false)
        );
        verifyNoInteractions(restClient);
    }

    @Test
    void getPortfolioAnalytics_shouldBypassOwnershipCheck_whenAdmin() {

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(stockHoldingRepository.findByUser_Id(10L)).thenReturn(List.of());

        Map<String, Object> result = analyticsService.getPortfolioAnalytics(10L, 999L, true);

        assertNotNull(result);
    }

}