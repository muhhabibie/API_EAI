package com.example.orderservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.orderservice.dto.ApiResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.service.OrderService;

import com.example.orderservice.dto.OrderRequestDTO;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/orders")
@Tag(name = "Order Management", description = "Endpoint untuk mengelola pemesanan barang")
public class OrderController {
    @Autowired
    private OrderService orderService;

    // ADMIN + USER bisa buat order
    @Operation(summary = "Buat Order Baru", description = "Membuat pesanan baru dengan daftar produk dan jumlah tertentu.")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> createOrder(@RequestBody OrderRequestDTO requestDTO) {
        Order created = orderService.createOrder(requestDTO.getCustomerId(), requestDTO.getItems());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order berhasil dibuat", created));
    }

    // ADMIN bisa lihat semua order, USER juga bisa (filter by customerId)
    @Operation(summary = "Ambil Semua Order", description = "Melihat riwayat pesanan (bisa difilter berdasarkan customerId).")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> getAllOrders(@RequestParam(required = false) Long customerId) {
        List<Order> orders;
        if (customerId != null) {
            orders = orderService.getOrdersByCustomer(customerId);
        } else {
            orders = orderService.getAllOrders();
        }
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    // Hanya ADMIN bisa update status order
    @Operation(summary = "Update Status Order", description = "Mengubah status pesanan secara manual (PENDING, PAID, dll). Khusus Admin.")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, 
            @io.swagger.v3.oas.annotations.Parameter(description = "Status: PENDING, PAID, PROCESSING, SHIPPED, DELIVERED, COMPLETED, CANCELLED")
            @RequestParam String status) {
        Order updated = orderService.updateStatus(id, status);
        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Order tidak ditemukan"));
        }
        return ResponseEntity.ok(ApiResponse.success("Status order berhasil diupdate", updated));
    }

    // ADMIN + USER bisa lihat detail order
    @Operation(summary = "Ambil Detail Order", description = "Melihat informasi lengkap satu pesanan termasuk item di dalamnya.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    // ADMIN + USER bisa cancel order
    @Operation(summary = "Batalkan Order", description = "Membatalkan pesanan yang belum dikirim dan mengembalikan stok ke gudang.")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
        Order cancelledOrder = orderService.cancelOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Order berhasil dibatalkan", cancelledOrder));
    }


    // Endpoint internal untuk menerima update dari Shipping Service
    @Operation(summary = "Update Status Pengiriman (Internal)", description = "Sinkronisasi status pesanan berdasarkan laporan dari Shipping Service.")
    @PutMapping("/{id}/shipping-status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updateShippingStatus(@PathVariable Long id, @RequestParam String status) {
        orderService.updateOrderStatusFromShipping(id, status);
        return ResponseEntity.ok(ApiResponse.success("Status order berhasil diupdate dari pengiriman", null));
    }
}
