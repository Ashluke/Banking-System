package com.banking.system.controller;

import com.banking.system.dto.response.StockQuoteResponseDto;
import com.banking.system.security.JWTService;
import com.banking.system.security.SecurityConfig;
import com.banking.system.services.StockService;

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

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StockController.class)
@Import(SecurityConfig.class)
public class StockControllerTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private StockService stockService;

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

    private StockQuoteResponseDto quoteResponse(String symbol) {
        return new StockQuoteResponseDto(symbol, 150.0, 2.5, 1.69, 155.0, 148.0, 149.0, 147.5);
    }


    // ===================== GET STOCKS =====================

    @Test
    void getStocks_shouldReturn200_whenUserRole() throws Exception {

        when(stockService.fetchQuotes(anyList()))
            .thenReturn(List.of(quoteResponse("AAPL"), quoteResponse("GOOGL")));

        mockMvc.perform(get("/api/market/stocks")
                .param("symbols", "AAPL", "GOOGL")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].symbol").value("AAPL"))
            .andExpect(jsonPath("$[1].symbol").value("GOOGL"));
    }

    @Test
    void getStocks_shouldReturn200_whenAdminRole() throws Exception {

        when(stockService.fetchQuotes(anyList()))
            .thenReturn(List.of(quoteResponse("TSLA")));

        mockMvc.perform(get("/api/market/stocks")
                .param("symbols", "TSLA")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].symbol").value("TSLA"))
            .andExpect(jsonPath("$[0].currentPrice").value(150.0));
    }

    @Test
    void getStocks_shouldReturn200_withEmptyList_whenSymbolsNotFound() throws Exception {

        when(stockService.fetchQuotes(anyList())).thenReturn(List.of());

        mockMvc.perform(get("/api/market/stocks")
                .param("symbols", "INVALID")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getStocks_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(get("/api/market/stocks")
                .param("symbols", "AAPL"))
            .andExpect(status().isUnauthorized());

        verify(stockService, never()).fetchQuotes(any());
    }

    @Test
    void getStocks_shouldReturn400_whenSymbolsMissing() throws Exception {

        mockMvc.perform(get("/api/market/stocks")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isBadRequest());

        verify(stockService, never()).fetchQuotes(any());
    }
}