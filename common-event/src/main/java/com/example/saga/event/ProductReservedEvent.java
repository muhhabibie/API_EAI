package com.example.saga.event;

import java.math.BigDecimal;

/**
 * Dipublish oleh: product-service
 * Dikonsumsi oleh: order-service, payment-service
 *
 * Dikirim saat stok berhasil direservasi. payment-service akan
 * otomatis memproses pembayaran saat menerima event ini.
 */
public record ProductReservedEvent(
        String orderId,
        Long customerId,
        java.util.List<OrderItemDTO> items,
        BigDecimal amount
) {}
