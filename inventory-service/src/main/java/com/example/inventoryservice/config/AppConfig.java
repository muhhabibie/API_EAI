package com.example.inventoryservice.config;

import org.springframework.context.annotation.Configuration;

/**
 * AppConfig - RestTemplate tidak lagi diperlukan karena Inventory Service
 * tidak lagi melakukan REST call ke Product Service.
 * Komunikasi antar service kini menggunakan Kafka event (product.stock.synced).
 */
@Configuration
public class AppConfig {
    // Tidak ada bean yang diperlukan
}
