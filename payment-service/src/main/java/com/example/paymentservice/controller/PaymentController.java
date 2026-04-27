package com.example.paymentservice.controller;

import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.service.PaymentService;
import com.example.paymentservice.dto.PaymentRequest;
import com.example.paymentservice.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
@Tag(name = "Payment Management", description = "Endpoint untuk mengelola transaksi pembayaran")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Operation(summary = "Proses Pembayaran", description = "Melakukan pembayaran untuk sebuah Order. Amount bisa dikosongkan untuk mengambil otomatis dari tagihan Order.")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> pay(@RequestBody PaymentRequest request) {
        Payment payment = paymentService.processPayment(
            request.getOrderId(), 
            request.getMethod()
        );
        
        return ResponseEntity.ok(ApiResponse.success("Pembayaran berhasil diproses", payment));
    }

    @Operation(summary = "Ambil Semua Riwayat Pembayaran", description = "Hanya dapat diakses oleh Admin untuk melihat seluruh transaksi.")
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getAllPayments() {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getAllPayments()));
    }

    @Operation(summary = "Ambil Detail Pembayaran by ID", description = "Melihat informasi lengkap satu transaksi pembayaran.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentById(id)));
    }

    @Operation(summary = "Ambil Riwayat Pembayaran by Order ID", description = "Mencari semua riwayat pembayaran yang terkait dengan sebuah Order.")
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> getPaymentByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentByOrderId(orderId)));
    }

    @Operation(summary = "Update Status Pembayaran", description = "Memperbarui status transaksi (misal: SUCCESS, FAILED, REFUNDED). Khusus Admin.")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updatePaymentStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success("Status pembayaran berhasil diperbarui", paymentService.updateStatus(id, status)));
    }

    @Operation(summary = "Hapus Catatan Pembayaran", description = "Menghapus permanen data pembayaran dari database. Khusus Admin.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> deletePayment(@PathVariable Long id) {
        paymentService.deletePayment(id);
        return ResponseEntity.ok(ApiResponse.success("Catatan pembayaran berhasil dihapus", null));
    }
}
