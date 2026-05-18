package com.example.saga.event;

/**
 * Dipublish oleh: order-service
 * Dikonsumsi oleh: (opsional - observer/audit service)
 *
 * Dikirim saat seluruh alur Saga berhasil diselesaikan:
 * stok sudah direservasi + payment sudah diproses.
 * Status order sudah COMPLETED saat event ini dipublish.
 */
public record OrderCompletedEvent(
        String orderId
) {}
