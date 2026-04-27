package com.example.inventoryservice.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.inventoryservice.dto.ApiResponse;
import com.example.inventoryservice.entity.InventoryReservation;
import com.example.inventoryservice.service.InventoryService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/inventory")
@Tag(name = "Inventory Management", description = "Endpoint untuk mengelola reservasi dan stok barang")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    // Hanya ADMIN bisa lihat semua reservasi
    @Operation(summary = "Ambil Semua Reservasi", description = "Melihat daftar seluruh stok barang yang sedang dikunci/direservasi oleh sistem. Khusus Admin.")
    @GetMapping("/reservations")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getAllReservations() {
        List<InventoryReservation> reservations = inventoryService.getAllReservations();
        return ResponseEntity.ok(ApiResponse.success(reservations));
    }
    
    // Hanya ADMIN bisa buat reservasi stok
    @Operation(summary = "Buat Reservasi Stok", description = "Mengunci sejumlah stok produk untuk pesanan tertentu secara manual. Khusus Admin.")
    @PostMapping("/reservations")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> reserveStock(@RequestBody Map<String, Object> payload) {
        Long productId = Long.valueOf(payload.get("productId").toString());
        Integer quantity = Integer.valueOf(payload.get("quantity").toString());
        String orderNumber = payload.containsKey("orderNumber") ? payload.get("orderNumber").toString() : "MANUAL-" + System.currentTimeMillis();
        
        InventoryReservation reservation = inventoryService.reserveStock(orderNumber, productId, quantity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Reservasi stok berhasil dibuat", reservation));
    }

    // Hanya ADMIN bisa hapus reservasi
    @Operation(summary = "Hapus/Lepas Reservasi", description = "Membatalkan reservasi stok dan mengembalikan jumlahnya ke stok utama. Khusus Admin.")
    @DeleteMapping("/reservations/{reservationId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> releaseReservation(@PathVariable Long reservationId) {
        inventoryService.releaseReservation(reservationId);
        return ResponseEntity.ok(ApiResponse.success("Reservasi berhasil dilepas", null));
    }
}
