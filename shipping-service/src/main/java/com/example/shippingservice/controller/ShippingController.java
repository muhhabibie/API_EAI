package com.example.shippingservice.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.shippingservice.dto.ApiResponse;
import com.example.shippingservice.dto.ShipmentRequest;
import com.example.shippingservice.entity.CourierType;
import com.example.shippingservice.entity.Shipment;
import com.example.shippingservice.service.ShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/shipments")
@CrossOrigin(origins = "*")
@Tag(name = "Shipping Management", description = "Endpoint untuk mengelola pengiriman barang")
public class ShippingController {

    @Autowired
    private ShippingService shippingService;

    @Operation(summary = "Buat Pengiriman Baru", description = "Mendaftarkan pengiriman barang baru untuk sebuah pesanan.")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> createShipment(@RequestBody ShipmentRequest request) {
        CourierType courier;
        try {
            courier = CourierType.valueOf(request.getCourierName().toUpperCase().replace(" ", "_"));
        } catch (Exception e) {
            courier = CourierType.JNE;
        }

        Shipment created = shippingService.createShipment(
            request.getOrderId(),
            courier,
            request.getReceiverName(),
            request.getDeliveryAddress(),
            request.getShippingFee()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Pengiriman untuk pesanan " + created.getOrderId() + " berhasil diproses dengan nomor resi " + created.getTrackingNumber(), created));
    }

    @Operation(summary = "Ambil Semua Pengiriman", description = "Melihat seluruh daftar pengiriman yang ada di sistem. Khusus Admin.")
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getAllShipments() {
        List<Shipment> shipments = shippingService.getAllShipments();
        return ResponseEntity.ok(ApiResponse.success(shipments));
    }

    @Operation(summary = "Ambil Pengiriman by ID", description = "Melihat detail informasi satu transaksi pengiriman tertentu.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> getShipment(@PathVariable Long id) {
        Shipment shipment = shippingService.getShipmentById(id);
        return ResponseEntity.ok(ApiResponse.success(shipment));
    }

    @Operation(summary = "Ambil Pengiriman by Order ID", description = "Mencari informasi pengiriman berdasarkan ID pesanan terkait.")
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> getShipmentByOrderId(@PathVariable Long orderId) {
        Shipment shipment = shippingService.getShipmentByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(shipment));
    }

    @Operation(summary = "Update Status Pengiriman", description = "Memperbarui status lokasi barang (PICKED_UP, IN_TRANSIT, dll). Khusus Admin.")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id, 
            @io.swagger.v3.oas.annotations.Parameter(description = "Status: PENDING, PICKED_UP, IN_TRANSIT, DELIVERED")
            @RequestParam String status) {
        Shipment updated = shippingService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Status pengiriman dengan resi " + updated.getTrackingNumber() + " berhasil diperbarui menjadi " + updated.getStatus(), updated));
    }
}
