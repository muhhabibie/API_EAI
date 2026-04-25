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

import com.example.inventoryservice.dto.ApiResponse;
import com.example.inventoryservice.entity.InventoryReservation;
import com.example.inventoryservice.service.InventoryService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    // Hanya ADMIN bisa lihat semua reservasi
    @GetMapping("/reservations")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getAllReservations() {
        List<InventoryReservation> reservations = inventoryService.getAllReservations();
        return ResponseEntity.ok(ApiResponse.success(reservations));
    }
    
    // Hanya ADMIN bisa buat reservasi stok
    @PostMapping("/reservations")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> reserveStock(@RequestBody Map<String, Object> payload) {
        Long productId = Long.valueOf(payload.get("productId").toString());
        Integer quantity = Integer.valueOf(payload.get("quantity").toString());
        InventoryReservation reservation = inventoryService.reserveStock(productId, quantity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Reservasi stok berhasil dibuat", reservation));
    }

    // Hanya ADMIN bisa hapus reservasi
    @DeleteMapping("/reservations/{reservationId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> releaseReservation(@PathVariable Long reservationId) {
        inventoryService.releaseReservation(reservationId);
        return ResponseEntity.ok(ApiResponse.success("Reservasi berhasil dilepas", null));
    }
}
