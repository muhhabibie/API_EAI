package com.example.saga.event;

/**
 * Dipublish oleh: order-service
 * Dikonsumsi oleh: (opsional - observer/audit service)
 *
 * Dikirim saat order di-CANCEL karena:
 *   1. Reservasi stok gagal (ProductReservationFailedEvent)
 *   2. Payment gagal (PaymentFailedEvent)
 *   3. Force failure setelah payment sukses (demo compensation)
 */
public record OrderCancelledEvent(
        String orderId,
        String reason
) {}
