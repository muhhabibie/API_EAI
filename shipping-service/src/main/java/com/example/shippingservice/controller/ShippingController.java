package com.example.shippingservice.controller;

import java.util.List;
import java.util.Map;

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

import com.example.shippingservice.dto.ApiResponse;
import com.example.shippingservice.entity.CourierType;
import com.example.shippingservice.entity.Shipment;
import com.example.shippingservice.service.ShippingService;

@RestController
@RequestMapping("/api/shipments")
@CrossOrigin(origins = "*")
public class ShippingController {

    @Autowired
    private ShippingService shippingService;

    // Hanya ADMIN bisa buat shipment
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> createShipment(@RequestBody Map<String, Object> payload) {
        Long orderId = Long.valueOf(payload.get("orderId").toString());
        String courierString = payload.get("courierName").toString().toUpperCase();
        CourierType courierEnum = CourierType.valueOf(courierString);
        
        Shipment created = shippingService.createShipment(orderId, courierEnum);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Shipment berhasil dibuat", created));
    }

    // Hanya ADMIN bisa lihat semua shipment
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getAllShipments() {
        List<Shipment> shipments = shippingService.getAllShipments();
        return ResponseEntity.ok(ApiResponse.success(shipments));
    }

    // ADMIN + USER bisa lihat detail shipment (tracking)
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> getShipment(@PathVariable Long id) {
        Shipment shipment = shippingService.getShipmentById(id);
        return ResponseEntity.ok(ApiResponse.success(shipment));
    }

    // ADMIN + USER bisa lihat shipment by order (tracking pesanan)
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> getShipmentByOrderId(@PathVariable Long orderId) {
        Shipment shipment = shippingService.getShipmentByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(shipment));
    }

    // Hanya ADMIN bisa update status shipment
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id, 
            @RequestParam String status) {
        Shipment updated = shippingService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Status shipment berhasil diupdate", updated));
    }
}
