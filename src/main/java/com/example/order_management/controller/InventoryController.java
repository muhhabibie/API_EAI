package com.example.order_management.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.order_management.entity.InventoryReservation;
import com.example.order_management.service.InventoryService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @GetMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> getStock(@PathVariable Long productId) {
        int stock = inventoryService.getStock(productId);
        return ResponseEntity.ok(Map.of("productId", productId, "stock", stock));
    }

    @PostMapping("/reservations")
    public ResponseEntity<InventoryReservation> reserveStock(@RequestBody Map<String, Object> payload) {
        Long productId = Long.valueOf(payload.get("productId").toString());
        Integer quantity = Integer.valueOf(payload.get("quantity").toString());
        InventoryReservation reservation = inventoryService.reserveStock(productId, quantity);
        return new ResponseEntity<>(reservation, HttpStatus.CREATED);
    }

    @DeleteMapping("/reservations/{reservationId}")
    public ResponseEntity<Map<String, String>> releaseReservation(@PathVariable Long reservationId) {
        inventoryService.releaseReservation(reservationId);
        return ResponseEntity.ok(Map.of("message", "Reservation released"));
    }
}