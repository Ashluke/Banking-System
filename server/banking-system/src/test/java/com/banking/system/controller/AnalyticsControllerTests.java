package com.banking.system.controller;

import com.banking.system.security.JWTService;
import com.banking.system.security.SecurityConfig;
import com.banking.system.services.AnalyticsService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalyticsController.class)
@Import(SecurityConfig.class)
public class AnalyticsControllerTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsService analyticsService;

    @MockitoBean
    private JWTService jwtService;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    private UsernamePasswordAuthenticationToken userAuth(Long appUserId) {
        return new UsernamePasswordAuthenticationToken(
            appUserId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private UsernamePasswordAuthenticationToken adminAuth(Long appUserId) {
        return new UsernamePasswordAuthenticationToken(
            appUserId, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }


    // ===================== GET /api/analytics/insights/{accountId} =====================

    @Test
    void getTransactionInsights_shouldReturn200_whenUser() throws Exception {

        when(analyticsService.getTransactionInsights(anyLong(), anyLong(), eq(false)))
            .thenReturn(Map.of("summary", Map.of("totalTransactions", 0)));

        mockMvc.perform(get("/api/analytics/insights/1")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isOk());
    }

    @Test
    void getTransactionInsights_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(get("/api/analytics/insights/1"))
            .andExpect(status().isUnauthorized());
    }


    // ===================== GET /api/analytics/trends/{accountId} =====================

    @Test
    void getSpendingTrends_shouldReturn200_whenUser() throws Exception {

        when(analyticsService.getSpendingTrends(anyLong(), anyLong(), eq(false)))
            .thenReturn(Map.of("spendingTrend", "INSUFFICIENT_DATA"));

        mockMvc.perform(get("/api/analytics/trends/1")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.spendingTrend").value("INSUFFICIENT_DATA"));
    }

    @Test
    void getSpendingTrends_shouldReturn200_whenAdmin() throws Exception {

        when(analyticsService.getSpendingTrends(anyLong(), anyLong(), eq(true)))
            .thenReturn(Map.of("spendingTrend", "STABLE"));

        mockMvc.perform(get("/api/analytics/trends/1")
                .with(authentication(adminAuth(999L))))
            .andExpect(status().isOk());
    }

    @Test
    void getSpendingTrends_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(get("/api/analytics/trends/1"))
            .andExpect(status().isUnauthorized());
    }


    // ===================== GET /api/analytics/fraud/{accountId} =====================

    @Test
    void detectFraud_shouldReturn200_whenUser() throws Exception {

        when(analyticsService.detectFraud(anyLong(), anyLong(), eq(false)))
            .thenReturn(Map.of("riskLevel", "LOW"));

        mockMvc.perform(get("/api/analytics/fraud/1")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.riskLevel").value("LOW"));
    }

    @Test
    void detectFraud_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(get("/api/analytics/fraud/1"))
            .andExpect(status().isUnauthorized());
    }


    // ===================== GET /api/analytics/predictions/{accountId} =====================

    @Test
    void predictSavings_shouldReturn200_whenUser() throws Exception {

        when(analyticsService.predictSavings(anyLong(), anyLong(), eq(false)))
            .thenReturn(Map.of("savingsTrend", "IMPROVING"));

        mockMvc.perform(get("/api/analytics/predictions/1")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.savingsTrend").value("IMPROVING"));
    }

    @Test
    void predictSavings_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(get("/api/analytics/predictions/1"))
            .andExpect(status().isUnauthorized());
    }


    // ===================== GET /api/analytics/credit-score/{accountId} =====================

    @Test
    void getCreditScore_shouldReturn200_whenUser() throws Exception {

        when(analyticsService.getCreditScore(anyLong(), anyLong(), eq(false)))
            .thenReturn(Map.of("creditScore", 680, "rating", "GOOD"));

        mockMvc.perform(get("/api/analytics/credit-score/1")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.creditScore").value(680))
            .andExpect(jsonPath("$.rating").value("GOOD"));
    }

    @Test
    void getCreditScore_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(get("/api/analytics/credit-score/1"))
            .andExpect(status().isUnauthorized());
    }


    // ===================== GET /api/analytics/portfolio/{userId} =====================

    @Test
    void getPortfolioAnalytics_shouldReturn200_whenUser() throws Exception {

        when(analyticsService.getPortfolioAnalytics(anyLong(), anyLong(), eq(false)))
            .thenReturn(Map.of("summary", Map.of("numberOfHoldings", 0)));

        mockMvc.perform(get("/api/analytics/portfolio/10")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isOk());
    }

    @Test
    void getPortfolioAnalytics_shouldReturn200_whenAdmin() throws Exception {

        when(analyticsService.getPortfolioAnalytics(anyLong(), anyLong(), eq(true)))
            .thenReturn(Map.of("summary", Map.of("numberOfHoldings", 2)));

        mockMvc.perform(get("/api/analytics/portfolio/10")
                .with(authentication(adminAuth(999L))))
            .andExpect(status().isOk());
    }

    @Test
    void getPortfolioAnalytics_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(get("/api/analytics/portfolio/10"))
            .andExpect(status().isUnauthorized());
    }
}