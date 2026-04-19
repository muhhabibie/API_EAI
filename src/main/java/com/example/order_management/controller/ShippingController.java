package com.example.order_management.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.order_management.entity.CourierType;
import com.example.order_management.entity.Shipment;
import com.example.order_management.service.ShippingService;

@RestController
@RequestMapping("/api/shipments")
@CrossOrigin(origins = "*")
public class ShippingController {

    @Autowired
    private ShippingService shippingService;

    @PostMapping
    public ResponseEntity<Shipment> createShipment(@RequestBody Map<String, Object> payload) {
        Long orderId = Long.valueOf(payload.get("orderId").toString());
        String courierString = payload.get("courierName").toString().toUpperCase();
        CourierType courierEnum = CourierType.valueOf(courierString);
        
        Shipment created = shippingService.createShipment(orderId, courierEnum);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<java.util.List<Shipment>> getAllShipments() {
        return ResponseEntity.ok(shippingService.getAllShipments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Shipment> getShipment(@PathVariable Long id) {
        return ResponseEntity.ok(shippingService.getShipmentById(id));
    }

    // FITUR BARU: Cari Shipment berdasarkan Order ID
    @GetMapping("/order/{orderId}")
    public ResponseEntity<Shipment> getShipmentByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(shippingService.getShipmentByOrderId(orderId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Shipment> updateStatus(
            @PathVariable Long id, 
            @RequestParam String status) {
        return ResponseEntity.ok(shippingService.updateStatus(id, status));
    }
}