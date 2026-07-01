package com.banking.system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean("finnhubClient")
    public RestClient finnhubClient() {
        return RestClient.builder()
            .baseUrl("https://finnhub.io/api/v1")
            .build();
    }

    @Bean("analyticsClient")
    public RestClient analyticsClient() {
        return RestClient.builder().build();
    }
}