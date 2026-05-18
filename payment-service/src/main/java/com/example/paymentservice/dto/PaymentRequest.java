package com.example.paymentservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class PaymentRequest {
    @Schema(example = "1", description = "ID dari Order yang akan dibayar")
    private Long orderId;
    
    @Schema(example = "BALANCE", description = "Metode pembayaran. (Misal: BALANCE, VA_MANDIRI, VA_BCA, GOPAY, OVO, CREDIT_CARD). Catatan: Saat ini semua metode akan memotong saldo Customer secara simulasi.")
    private String method;

    @Schema(example = "REF-12345", description = "Nomor referensi unik dari client untuk mencegah pembayaran ganda (Idempotency)")
    private String referenceNumber;

    // Getters and Setters
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
}
