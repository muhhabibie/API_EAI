package com.example.saga.event;

import java.math.BigDecimal;

/**
 * Dipublish oleh: payment-service
 * Dikonsumsi oleh: order-service
 *
 * Dikirim saat pembayaran ditolak (misal: melebihi batas gateway).
 * order-service akan CANCEL order dan trigger ReleaseProductReservationEvent
 * sebagai compensation untuk mengembalikan stok.
 */
public record PaymentFailedEvent(
        String orderId,
        BigDecimal amount,
        String reason
) {}
