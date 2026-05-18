package com.example.saga.event;

import java.math.BigDecimal;

/**
 * Dipublish oleh: order-service (sebagai COMPENSATION)
 * Dikonsumsi oleh: payment-service
 *
 * Dikirim HANYA pada skenario force failure setelah payment sukses.
 * payment-service akan mengubah status payment menjadi REFUNDED.
 */
public record RefundPaymentEvent(
        String orderId,
        BigDecimal amount,
        String reason
) {}
