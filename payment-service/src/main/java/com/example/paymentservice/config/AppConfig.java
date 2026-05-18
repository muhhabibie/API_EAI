package com.example.paymentservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    /**
     * Singleton RestTemplate — dibuat sekali, dipakai oleh semua service.
     * Lebih efisien daripada new RestTemplate() setiap method call.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
