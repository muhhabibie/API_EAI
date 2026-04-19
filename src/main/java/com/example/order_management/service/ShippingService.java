package com.example.order_management.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.order_management.entity.CourierType; 
import com.example.order_management.entity.Shipment;
import com.example.order_management.repository.ShipmentRepository;

@Service
public class ShippingService {

    @Autowired
    private ShipmentRepository shipmentRepository;

    // @Lazy Mencegah Error Circular Dependency dengan OrderService
    @Autowired
    @Lazy 
    private OrderService orderService;

    @Transactional
    public Shipment createShipment(Long orderId, CourierType courier) {
        if (shipmentRepository.findByOrderId(orderId).isPresent()) {
            throw new RuntimeException("Shipment sudah ada untuk Order ID: " + orderId);
        }

        Shipment shipment = new Shipment();
        shipment.setOrderId(orderId);
        shipment.setCourierName(courier); 
        shipment.setStatus("PENDING");
        shipment.setTrackingNumber("IFS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        
        return shipmentRepository.save(shipment);
    }

    public Shipment getShipmentByOrderId(Long orderId) {
        return shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Shipment tidak ditemukan untuk order ID: " + orderId));
    }

    @Transactional
    public Shipment updateStatus(Long shipmentId, String newStatus) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment dengan ID " + shipmentId + " tidak ditemukan"));
        
        shipment.setStatus(newStatus.toUpperCase());
        
        if ("IN_TRANSIT".equals(shipment.getStatus())) {
            shipment.setShippedAt(LocalDateTime.now());
        } else if ("DELIVERED".equals(shipment.getStatus())) {
            shipment.setDeliveredAt(LocalDateTime.now());
        }
        
        Shipment savedShipment = shipmentRepository.save(shipment);

        // INTEGRASI: Sinkronkan status ke OrderService
        orderService.updateOrderStatusFromShipping(savedShipment.getOrderId(), savedShipment.getStatus());

        return savedShipment;
    }

    public Shipment getShipmentById(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shipment tidak ditemukan untuk ID: " + id));
    }

    public java.util.List<Shipment> getAllShipments() {
        return shipmentRepository.findAll();
    }
}