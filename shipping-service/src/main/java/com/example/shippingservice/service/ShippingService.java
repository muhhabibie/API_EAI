package com.example.shippingservice.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shippingservice.entity.CourierType; 
import com.example.shippingservice.entity.Shipment;
import com.example.shippingservice.repository.ShipmentRepository;

@Service
public class ShippingService {

    @Autowired
    private ShipmentRepository shipmentRepository;

    // ========== STATUS CONSTANTS ==========
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PICKED_UP = "PICKED_UP";
    public static final String STATUS_IN_TRANSIT = "IN_TRANSIT";
    public static final String STATUS_DELIVERED = "DELIVERED";

    // ========== CREATE SHIPMENT ==========
    @Transactional
    public Shipment createShipment(Long orderId, CourierType courier) {
        if (shipmentRepository.findByOrderId(orderId).isPresent()) {
            throw new RuntimeException("Shipment sudah ada untuk Order ID: " + orderId);
        }

        Shipment shipment = new Shipment();
        shipment.setOrderId(orderId);
        shipment.setCourierName(courier); 
        shipment.setStatus(STATUS_PENDING);
        shipment.setTrackingNumber("IFS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        
        return shipmentRepository.save(shipment);
    }

    // ========== UPDATE STATUS ==========
    // Validasi transisi: PENDING → PICKED_UP → IN_TRANSIT → DELIVERED
    @Transactional
    public Shipment updateStatus(Long shipmentId, String newStatus) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment dengan ID " + shipmentId + " tidak ditemukan"));
        
        String currentStatus = shipment.getStatus();
        newStatus = newStatus.toUpperCase();

        // Validasi transisi status
        boolean validTransition = switch (newStatus) {
            case STATUS_PICKED_UP -> STATUS_PENDING.equals(currentStatus);
            case STATUS_IN_TRANSIT -> STATUS_PICKED_UP.equals(currentStatus);
            case STATUS_DELIVERED -> STATUS_IN_TRANSIT.equals(currentStatus);
            default -> false;
        };

        if (!validTransition) {
            throw new RuntimeException("Transisi status tidak valid: " + currentStatus + " → " + newStatus
                    + ". Alur yang benar: PENDING → PICKED_UP → IN_TRANSIT → DELIVERED");
        }

        shipment.setStatus(newStatus);
        
        if (STATUS_PICKED_UP.equals(newStatus) || STATUS_IN_TRANSIT.equals(newStatus)) {
            shipment.setShippedAt(LocalDateTime.now());
        } else if (STATUS_DELIVERED.equals(newStatus)) {
            shipment.setDeliveredAt(LocalDateTime.now());
        }
        
        return shipmentRepository.save(shipment);
    }

    // ========== QUERIES ==========
    public Shipment getShipmentByOrderId(Long orderId) {
        return shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Shipment tidak ditemukan untuk order ID: " + orderId));
    }

    public Shipment getShipmentById(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shipment tidak ditemukan untuk ID: " + id));
    }

    public java.util.List<Shipment> getAllShipments() {
        return shipmentRepository.findAll();
    }
}
