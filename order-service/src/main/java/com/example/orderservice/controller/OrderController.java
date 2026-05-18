package com.example.orderservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.orderservice.dto.ApiResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.service.OrderService;
import com.example.orderservice.dto.OrderRequestDTO;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/orders")
@Tag(
    name = "Order Management",
    description = "Endpoint untuk membuat dan mengelola pesanan. Setelah order dibuat, " +
                  "sistem Saga otomatis memproses: (1) Reservasi stok, (2) Pembayaran dari saldo customer, " +
                  "(3) Pembuatan data pengiriman."
)
public class OrderController {
    @Autowired
    private OrderService orderService;

    @Operation(
        summary = "Buat Order Baru",
        description = "Membuat pesanan baru. Setelah berhasil, sistem Saga Choreography otomatis berjalan: " +
                      "stok direservasi → saldo dipotong → pengiriman dibuat. " +
                      "Status order akan berubah: PENDING → AWAITING_PAYMENT → PAID."
    )
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> createOrder(@RequestBody OrderRequestDTO requestDTO) {
        Order created = orderService.createOrder(requestDTO.getCustomerId(), requestDTO.getItems());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Pesanan berhasil dibuat dengan nomor order: " + created.getOrderNumber(), created));
    }

    @Operation(
        summary = "Ambil Semua Order",
        description = "Melihat riwayat pesanan. Filter dengan customerId untuk melihat order milik customer tertentu."
    )
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> getAllOrders(
        @Parameter(description = "Filter berdasarkan ID customer. Kosongkan untuk melihat semua order (Admin).", example = "1")
        @RequestParam(required = false) Long customerId) {
        List<Order> orders;
        if (customerId != null) {
            orders = orderService.getOrdersByCustomer(customerId);
        } else {
            orders = orderService.getAllOrders();
        }
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @Operation(
        summary = "Update Status Order (Admin)",
        description = "Mengubah status pesanan secara manual. Hanya untuk koreksi data oleh Admin. " +
                      "Status valid: PENDING, AWAITING_PAYMENT, PAID, PROCESSING, SHIPPED, DELIVERED, COMPLETED, CANCELLED."
    )
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updateStatus(
            @Parameter(description = "ID Order yang akan diupdate.", example = "1") @PathVariable Long id,
            @Parameter(description = "Status baru order.", example = "PAID",
                schema = @io.swagger.v3.oas.annotations.media.Schema(
                    allowableValues = {"PENDING", "AWAITING_PAYMENT", "PAID", "PROCESSING", "SHIPPED", "DELIVERED", "COMPLETED", "CANCELLED"}))
            @RequestParam String status) {
        // FIX isu #1: Hapus dead null-check — updateStatus() selalu throw exception
        // jika order tidak ada (via .orElseThrow()), tidak pernah return null.
        // Error handling ditangani oleh GlobalExceptionHandler.
        Order updated = orderService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Status pesanan " + updated.getOrderNumber() + " berhasil diperbarui menjadi " + updated.getStatus(), updated));
    }

    @Operation(
        summary = "Ambil Detail Order",
        description = "Melihat informasi lengkap satu pesanan termasuk item produk, total harga, dan status terkini."
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> getOrderById(
            @Parameter(description = "ID Order yang ingin dilihat.", example = "1") @PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @Operation(
        summary = "Batalkan Order",
        description = "Membatalkan pesanan yang belum berstatus PAID. " +
                      "Sistem otomatis mengembalikan stok ke gudang via Kafka. " +
                      "Untuk membatalkan order yang sudah PAID, gunakan endpoint /cancel-after-payment."
    )
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> cancelOrder(
            @Parameter(description = "ID Order yang akan dibatalkan.", example = "1") @PathVariable Long id) {
        Order cancelledOrder = orderService.cancelOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Pesanan " + cancelledOrder.getOrderNumber() + " berhasil dibatalkan", cancelledOrder));
    }


    @Operation(
        summary = "[INTERNAL] Sinkronisasi Status Pengiriman",
        description = "Endpoint internal — dipanggil oleh Shipping Service untuk memperbarui status order " +
                      "saat status pengiriman berubah (SHIPPED / DELIVERED). Tidak perlu dipanggil manual."
    )
    @PutMapping("/{id}/shipping-status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updateShippingStatus(
            @Parameter(description = "ID Order.", example = "1") @PathVariable Long id,
            @Parameter(description = "Status pengiriman dari kurir.", example = "DELIVERED",
                schema = @io.swagger.v3.oas.annotations.media.Schema(
                    allowableValues = {"SHIPPED", "DELIVERED"})) @RequestParam String status) {
        orderService.updateOrderStatusFromShipping(id, status);
        return ResponseEntity.ok(ApiResponse.success("Status pesanan " + id + " otomatis diperbarui berdasarkan informasi pengiriman menjadi " + status, null));
    }

    // ============================================================
    // Endpoint untuk membatalkan order yang sudah dalam status PAID.
    // Sistem otomatis akan:
    //   1. Mengembalikan stok ke inventory via Kafka
    //   2. Memproses refund pembayaran ke payment-service via REST
    //   3. Mengubah status order menjadi CANCELLED
    // ============================================================
    @Operation(
        summary = "Batalkan Order yang Sudah Dibayar (Refund)",
        description = "Membatalkan order berstatus PAID sebelum dikirim. " +
                      "Sistem otomatis: (1) Mengembalikan stok ke inventory, " +
                      "(2) Memproses refund saldo ke customer, " +
                      "(3) Mengubah status order menjadi CANCELLED. " +
                      "Proses refund berjalan asinkron via Kafka — cek saldo customer beberapa saat setelah request."
    )
    @PatchMapping("/{id}/cancel-after-payment")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> cancelPaidOrder(
            @Parameter(description = "ID Order berstatus PAID yang akan dibatalkan.", example = "1") @PathVariable Long id) {
        Order order = orderService.cancelPaidOrder(id);
        return ResponseEntity.ok(ApiResponse.success(
                "Pesanan " + order.getOrderNumber() + " berhasil dibatalkan. Proses sinkronisasi refund dan pengembalian stok sedang berlangsung.", order));
    }
}
