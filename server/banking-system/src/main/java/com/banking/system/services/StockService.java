package com.banking.system.services;

import com.banking.system.dto.response.StockQuoteResponseDto;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class StockService {

    @Value("${finnhub.api.key}")
    private String apiKey;

    private final RestClient restClient;

    public StockService(@Qualifier("finnhubClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public List<StockQuoteResponseDto> fetchQuotes(List<String> symbols) {

        List<StockQuoteResponseDto> quotes = new ArrayList<>();

        for (String symbol : symbols) {

            String trimmed = symbol.trim().toUpperCase();

            if (trimmed.isEmpty()) continue;

            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> response = restClient.get()
                    .uri("/quote?symbol={symbol}&token={token}", trimmed, apiKey)
                    .retrieve()
                    .body(Map.class);

                if (response == null) continue;

                double currentPrice  = toDouble(response.get("c"));
                double change        = toDouble(response.get("d"));
                double percentChange = toDouble(response.get("dp"));
                double highPrice     = toDouble(response.get("h"));
                double lowPrice      = toDouble(response.get("l"));
                double openPrice     = toDouble(response.get("o"));
                double previousClose = toDouble(response.get("pc"));

                // Skip if Finnhub returned no data for the symbol
                if (currentPrice == 0.0 && previousClose == 0.0) continue;

                quotes.add(new StockQuoteResponseDto(
                    trimmed,
                    currentPrice,
                    change,
                    percentChange,
                    highPrice,
                    lowPrice,
                    openPrice,
                    previousClose
                ));

            } catch (Exception e) {
                // Skip failed symbols
            }
        }

        return quotes;
    }

    private double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return 0.0;
    }
}