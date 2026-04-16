package com.example.order_management.controller;

import com.example.order_management.entity.InventoryReservation;
import com.example.order_management.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @GetMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> getStock(@PathVariable Long productId) {
        int stock = inventoryService.getStock(productId);
        return ResponseEntity.ok(Map.of("productId", productId, "stock", stock));
    }

    @PostMapping("/reserve")
    public ResponseEntity<InventoryReservation> reserveStock(@RequestBody Map<String, Object> payload) {
        Long productId = Long.valueOf(payload.get("productId").toString());
        Integer quantity = Integer.valueOf(payload.get("quantity").toString());
        InventoryReservation reservation = inventoryService.reserveStock(productId, quantity);
        return new ResponseEntity<>(reservation, HttpStatus.CREATED);
    }

    @DeleteMapping("/reserve/{reservationId}")
    public ResponseEntity<Map<String, String>> releaseReservation(@PathVariable Long reservationId) {
        inventoryService.releaseReservation(reservationId);
        return ResponseEntity.ok(Map.of("message", "Reservation released"));
    }
}