package com.example.saga.event;

/**
 * Event yang dipublish oleh:
 * - Product Service: saat admin menambah/update produk (reason = PRODUCT_UPDATED)
 * - Inventory Service: saat pembayaran dikonfirmasi, stok fisik berkurang (reason = PAYMENT_CONFIRMED)
 *
 * Dikonsumsi oleh:
 * - Inventory Service: untuk sinkronisasi totalQty di tabel inventory
 * - Product Service: untuk sinkronisasi kolom stock di tabel products
 */
public record ProductStockSyncedEvent(
        Long productId,
        int newStock,
        String reason   // "PRODUCT_UPDATED" | "PAYMENT_CONFIRMED"
) {}
