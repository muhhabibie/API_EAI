package com.example.saga.event;

/**
 * Dipublish oleh: order-service (sebagai COMPENSATION)
 * Dikonsumsi oleh: product-service
 *
 * Dikirim saat order di-CANCEL setelah stok berhasil direservasi.
 * Terjadi pada dua skenario:
 *   1. Payment gagal → order-service publish event ini untuk rollback stok
 *   2. Force failure setelah payment sukses (demo compensation)
 */
public record ReleaseProductReservationEvent(
        String orderId,
        java.util.List<OrderItemDTO> items,
        String reason
) {}
