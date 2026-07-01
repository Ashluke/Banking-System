package com.banking.system.services;

import com.banking.system.dto.analytics.AccountAnalyticsData;
import com.banking.system.dto.analytics.CreditScoreRequest;
import com.banking.system.dto.analytics.PortfolioAnalyticsRequest;
import com.banking.system.dto.analytics.TransactionAnalyticsRequest;
import com.banking.system.dto.request.StockHoldingAnalyticsDto;
import com.banking.system.dto.response.StockQuoteResponseDto;
import com.banking.system.dto.response.TransactionResponseDto;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.exception.UnauthorizedActionException;
import com.banking.system.model.entities.BankAccount;
import com.banking.system.model.entities.StockHolding;
import com.banking.system.model.entities.User;
import com.banking.system.repository.BankAccountRepository;
import com.banking.system.repository.JointAccountMemberRepository;
import com.banking.system.repository.StockHoldingRepository;
import com.banking.system.repository.TransactionRepository;
import com.banking.system.repository.UserRepository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    @Value("${analytics.service.url}")
    private String analyticsServiceUrl;

    private final RestClient restClient;
    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final StockHoldingRepository stockHoldingRepository;
    private final JointAccountMemberRepository jointAccountMemberRepository;
    private final StockService stockService;

    public AnalyticsService(
            @Qualifier("analyticsClient") RestClient restClient,
            UserRepository userRepository,
            BankAccountRepository bankAccountRepository,
            TransactionRepository transactionRepository,
            StockHoldingRepository stockHoldingRepository,
            JointAccountMemberRepository jointAccountMemberRepository,
            StockService stockService) {
        this.restClient = restClient;
        this.userRepository = userRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.stockHoldingRepository = stockHoldingRepository;
        this.jointAccountMemberRepository = jointAccountMemberRepository;
        this.stockService = stockService;
    }

    // Transaction insights

    public Map<String, Object> getTransactionInsights(Long accountId, Long appUserId, boolean isAdmin) {

        BankAccount account = bankAccountRepository.findById(accountId).orElseThrow(() ->
            new ResourceNotFoundException("Account not found"));

        checkOwnershipOrAdmin(account, appUserId, isAdmin);

        TransactionAnalyticsRequest payload = buildTransactionPayload(account);

        return callPython("/analytics/transactions/insights", payload);
    }

    // Spending trends

    public Map<String, Object> getSpendingTrends(Long accountId, Long appUserId, boolean isAdmin) {

        BankAccount account = bankAccountRepository.findById(accountId).orElseThrow(() ->
            new ResourceNotFoundException("Account not found"));

        checkOwnershipOrAdmin(account, appUserId, isAdmin);

        TransactionAnalyticsRequest payload = buildTransactionPayload(account);

        return callPython("/analytics/transactions/trends", payload);
    }

    // Fraud detection

    public Map<String, Object> detectFraud(Long accountId, Long appUserId, boolean isAdmin) {

        BankAccount account = bankAccountRepository.findById(accountId).orElseThrow(() ->
            new ResourceNotFoundException("Account not found"));

        checkOwnershipOrAdmin(account, appUserId, isAdmin);

        TransactionAnalyticsRequest payload = buildTransactionPayload(account);

        return callPython("/analytics/fraud/detect", payload);
    }

    // Savings prediction

    public Map<String, Object> predictSavings(Long accountId, Long appUserId, boolean isAdmin) {

        BankAccount account = bankAccountRepository.findById(accountId).orElseThrow(() ->
            new ResourceNotFoundException("Account not found"));

        checkOwnershipOrAdmin(account, appUserId, isAdmin);

        TransactionAnalyticsRequest payload = buildTransactionPayload(account);

        return callPython("/analytics/predictions/savings", payload);
    }

    // Credit score

    public Map<String, Object> getCreditScore(Long accountId, Long appUserId, boolean isAdmin) {

        BankAccount account = bankAccountRepository.findById(accountId).orElseThrow(() ->
            new ResourceNotFoundException("Account not found"));

        checkOwnershipOrAdmin(account, appUserId, isAdmin);

        TransactionAnalyticsRequest transactionData = buildTransactionPayload(account);

        AccountAnalyticsData accountData = new AccountAnalyticsData(
            account.getId(),
            account.getUser().getId(),
            account.getBalance().doubleValue(),
            account.getStatus().name(),
            account.getUser().getAppUser().getCreatedAt().toString()
        );

        CreditScoreRequest payload = new CreditScoreRequest(transactionData, accountData);

        return callPython("/analytics/credit-score", payload);
    }

    // Portfolio analytics

    public Map<String, Object> getPortfolioAnalytics(Long userId, Long appUserId, boolean isAdmin) {

        User user = userRepository.findById(userId).orElseThrow(() ->
            new ResourceNotFoundException("User not found"));

        if (!isAdmin && !user.getAppUser().getId().equals(appUserId)) {
            throw new UnauthorizedActionException("You do not have access to this portfolio");
        }

        List<StockHolding> holdings = stockHoldingRepository.findByUser_Id(userId);

        if (holdings.isEmpty()) {
            return Map.of(
                "summary", Map.of("numberOfHoldings", 0),
                "holdings", List.of(),
                "insight", "No holdings found. Add stock holdings to see portfolio analytics."
            );
        }

        List<String> symbols = holdings.stream()
            .map(StockHolding::getSymbol)
            .collect(Collectors.toList());

        List<StockQuoteResponseDto> quotes = stockService.fetchQuotes(symbols);

        Map<String, Double> priceMap = quotes.stream()
            .collect(Collectors.toMap(
                StockQuoteResponseDto::getSymbol,
                StockQuoteResponseDto::getCurrentPrice
            ));

        List<StockHoldingAnalyticsDto> analyticsHoldings = holdings.stream()
            .map(h -> new StockHoldingAnalyticsDto(
                h.getId(),
                user.getId(),
                h.getSymbol(),
                h.getQuantity(),
                h.getPurchasePrice(),
                BigDecimal.valueOf(priceMap.getOrDefault(h.getSymbol(), h.getPurchasePrice().doubleValue())),
                h.getPurchasedAt()
            ))
            .collect(Collectors.toList());

        PortfolioAnalyticsRequest payload = new PortfolioAnalyticsRequest(userId, analyticsHoldings);

        return callPython("/analytics/portfolio/performance", payload);
    }

    // Helpers

    private TransactionAnalyticsRequest buildTransactionPayload(BankAccount account) {

        List<TransactionResponseDto> transactions = transactionRepository
            .findByBankAccount_Id(account.getId(), Pageable.unpaged())
            .getContent()
            .stream()
            .map(tx -> new TransactionResponseDto(
                tx.getId(),
                tx.getBankAccount().getId(),
                tx.getRelatedTransactionId(),
                tx.getAmount(),
                tx.getType(),
                tx.getTimestamp()
            ))
            .collect(Collectors.toList());

        return new TransactionAnalyticsRequest(
            account.getUser().getId(),
            account.getBalance().doubleValue(),
            transactions
        );
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> callPython(String path, Object payload) {
        return restClient.post()
            .uri(analyticsServiceUrl + path)
            .body(payload)
            .retrieve()
            .body(Map.class);
    }
}