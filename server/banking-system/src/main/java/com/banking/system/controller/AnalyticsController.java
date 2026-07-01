package com.banking.system.controller;

import com.banking.system.security.SecurityUtil;
import com.banking.system.services.AnalyticsService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/insights/{accountId}")
    public Map<String, Object> getTransactionInsights(@PathVariable Long accountId) {
        Long appUserId = SecurityUtil.getCurrentUserId();
        boolean isAdmin = SecurityUtil.isAdmin();
        return analyticsService.getTransactionInsights(accountId, appUserId, isAdmin);
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/trends/{accountId}")
    public Map<String, Object> getSpendingTrends(@PathVariable Long accountId) {
        Long appUserId = SecurityUtil.getCurrentUserId();
        boolean isAdmin = SecurityUtil.isAdmin();
        return analyticsService.getSpendingTrends(accountId, appUserId, isAdmin);
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/fraud/{accountId}")
    public Map<String, Object> detectFraud(@PathVariable Long accountId) {
        Long appUserId = SecurityUtil.getCurrentUserId();
        boolean isAdmin = SecurityUtil.isAdmin();
        return analyticsService.detectFraud(accountId, appUserId, isAdmin);
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/predictions/{accountId}")
    public Map<String, Object> predictSavings(@PathVariable Long accountId) {
        Long appUserId = SecurityUtil.getCurrentUserId();
        boolean isAdmin = SecurityUtil.isAdmin();
        return analyticsService.predictSavings(accountId, appUserId, isAdmin);
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/credit-score/{accountId}")
    public Map<String, Object> getCreditScore(@PathVariable Long accountId) {
        Long appUserId = SecurityUtil.getCurrentUserId();
        boolean isAdmin = SecurityUtil.isAdmin();
        return analyticsService.getCreditScore(accountId, appUserId, isAdmin);
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/portfolio/{userId}")
    public Map<String, Object> getPortfolioAnalytics(@PathVariable Long userId) {
        Long appUserId = SecurityUtil.getCurrentUserId();
        boolean isAdmin = SecurityUtil.isAdmin();
        return analyticsService.getPortfolioAnalytics(userId, appUserId, isAdmin);
    }
}