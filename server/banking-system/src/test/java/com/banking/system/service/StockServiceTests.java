package com.banking.system.service;

import com.banking.system.dto.response.StockQuoteResponseDto;
import com.banking.system.services.StockService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTests {

    private RestClient restClient;
    private RestClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;
    private RestClient.RequestHeadersSpec<?> requestHeadersSpec;
    private RestClient.ResponseSpec responseSpec;

    private StockService stockService;

    @BeforeEach
    void setup() {
        restClient = mock(RestClient.class);
        requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        stockService = new StockService(restClient);
    }

    private void mockRestClientResponse(Map<String, Object> responseBody) {
        doReturn(requestHeadersUriSpec).when(restClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(anyString(), any(), any());
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        doReturn(responseBody).when(responseSpec).body(Map.class);
    }

    // ===================== FETCH QUOTES =====================

    @Test
    void fetchQuotes_shouldReturnQuote_whenSymbolIsValid() {
        Map<String, Object> response = Map.of(
            "c",  150.0,
            "d",  2.5,
            "dp", 1.69,
            "h",  152.0,
            "l",  148.0,
            "o",  149.0,
            "pc", 147.5
        );
        mockRestClientResponse(response);

        List<StockQuoteResponseDto> result = stockService.fetchQuotes(List.of("AAPL"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSymbol()).isEqualTo("AAPL");
        assertThat(result.get(0).getCurrentPrice()).isEqualTo(150.0);
        assertThat(result.get(0).getChange()).isEqualTo(2.5);
        assertThat(result.get(0).getPercentChange()).isEqualTo(1.69);
        assertThat(result.get(0).getHighPrice()).isEqualTo(152.0);
        assertThat(result.get(0).getLowPrice()).isEqualTo(148.0);
        assertThat(result.get(0).getOpenPrice()).isEqualTo(149.0);
        assertThat(result.get(0).getPreviousClose()).isEqualTo(147.5);
    }

    @Test
    void fetchQuotes_shouldNormalizeSymbolToUppercase() {
        Map<String, Object> response = Map.of(
            "c", 150.0, "d", 0.0, "dp", 0.0,
            "h", 0.0, "l", 0.0, "o", 0.0, "pc", 100.0
        );
        mockRestClientResponse(response);

        List<StockQuoteResponseDto> result = stockService.fetchQuotes(List.of("aapl"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSymbol()).isEqualTo("AAPL");
    }

    @Test
    void fetchQuotes_shouldTrimWhitespaceFromSymbol() {
        Map<String, Object> response = Map.of(
            "c", 150.0, "d", 0.0, "dp", 0.0,
            "h", 0.0, "l", 0.0, "o", 0.0, "pc", 100.0
        );
        mockRestClientResponse(response);

        List<StockQuoteResponseDto> result = stockService.fetchQuotes(List.of("  AAPL  "));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSymbol()).isEqualTo("AAPL");
    }

    @Test
    void fetchQuotes_shouldReturnMultipleQuotes_whenMultipleSymbolsGiven() {
        Map<String, Object> response = Map.of(
            "c", 150.0, "d", 0.0, "dp", 0.0,
            "h", 0.0, "l", 0.0, "o", 0.0, "pc", 100.0
        );
        mockRestClientResponse(response);

        List<StockQuoteResponseDto> result = stockService.fetchQuotes(List.of("AAPL", "GOOGL", "MSFT"));

        assertThat(result).hasSize(3);
    }

    @Test
    void fetchQuotes_shouldSkipSymbol_whenFinnhubReturnsZeroData() {
        Map<String, Object> response = Map.of(
            "c", 0.0, "d", 0.0, "dp", 0.0,
            "h", 0.0, "l", 0.0, "o", 0.0, "pc", 0.0
        );
        mockRestClientResponse(response);

        List<StockQuoteResponseDto> result = stockService.fetchQuotes(List.of("INVALIDSYMBOL"));

        assertThat(result).isEmpty();
    }

    @Test
    void fetchQuotes_shouldSkipSymbol_whenResponseIsNull() {
        doReturn(requestHeadersUriSpec).when(restClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(anyString(), any(), any());
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        doReturn(null).when(responseSpec).body(Map.class);

        List<StockQuoteResponseDto> result = stockService.fetchQuotes(List.of("AAPL"));

        assertThat(result).isEmpty();
    }

    @Test
    void fetchQuotes_shouldSkipSymbol_whenApiThrowsException() {
        doReturn(requestHeadersUriSpec).when(restClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(anyString(), any(), any());
        doThrow(new RuntimeException("Finnhub unreachable")).when(requestHeadersSpec).retrieve();

        List<StockQuoteResponseDto> result = stockService.fetchQuotes(List.of("AAPL"));

        assertThat(result).isEmpty();
    }

    @Test
    void fetchQuotes_shouldContinue_whenOneSymbolFailsAndOtherSucceeds() {
        Map<String, Object> validResponse = Map.of(
            "c", 150.0, "d", 0.0, "dp", 0.0,
            "h", 0.0, "l", 0.0, "o", 0.0, "pc", 100.0
        );

        doReturn(requestHeadersUriSpec).when(restClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(anyString(), any(), any());
        doThrow(new RuntimeException("Finnhub unreachable"))
            .doReturn(responseSpec)
            .when(requestHeadersSpec).retrieve();
        doReturn(validResponse).when(responseSpec).body(Map.class);

        List<StockQuoteResponseDto> result = stockService.fetchQuotes(List.of("BADSYMBOL", "AAPL"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSymbol()).isEqualTo("AAPL");
    }

    @Test
    void fetchQuotes_shouldReturnEmpty_whenSymbolListIsEmpty() {
        List<StockQuoteResponseDto> result = stockService.fetchQuotes(List.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(restClient);
    }

    @Test
    void fetchQuotes_shouldSkipBlankSymbols() {
        List<StockQuoteResponseDto> result = stockService.fetchQuotes(List.of("  ", ""));

        assertThat(result).isEmpty();
        verifyNoInteractions(restClient);
    }

    @Test
    void fetchQuotes_shouldHandleNullFieldsInResponse() {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("c", 150.0);
        response.put("d", null);
        response.put("dp", null);
        response.put("h", null);
        response.put("l", null);
        response.put("o", null);
        response.put("pc", 100.0);

        mockRestClientResponse(response);

        List<StockQuoteResponseDto> result = stockService.fetchQuotes(List.of("AAPL"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChange()).isEqualTo(0.0);
        assertThat(result.get(0).getPercentChange()).isEqualTo(0.0);
    }
}