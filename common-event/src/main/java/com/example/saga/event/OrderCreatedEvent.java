package com.example.saga.event;

import java.math.BigDecimal;

/**
 * Dipublish oleh: order-service
 * Dikonsumsi oleh: product-service
 *
 * Dikirim saat order baru dibuat. product-service akan mencoba
 * mereservasi stok berdasarkan event ini.
 */
public record OrderCreatedEvent(
        String orderId,
        Long customerId,
        java.util.List<OrderItemDTO> items,
        BigDecimal amount
) {}
