package com.example.saga.event;

/**
 * Dipublish oleh: product-service
 * Dikonsumsi oleh: order-service
 *
 * Dikirim saat stok tidak mencukupi atau produk tidak ditemukan.
 * order-service akan mengubah status order menjadi CANCELLED.
 */
public record ProductReservationFailedEvent(
        String orderId,
        java.util.List<OrderItemDTO> items,
        String reason
) {}
