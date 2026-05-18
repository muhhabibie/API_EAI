package com.example.saga.event;

import java.math.BigDecimal;

/**
 * Dipublish oleh: payment-service
 * Dikonsumsi oleh: order-service
 *
 * Dikirim saat pembayaran berhasil diproses.
 * order-service akan mengubah status order menjadi COMPLETED.
 */
public record PaymentProcessedEvent(
        String orderId,
        BigDecimal amount,
        String reference
) {}
