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

    @Autowired
    private com.example.shippingservice.security.JwtUtil jwtUtil;

    // ========== STATUS CONSTANTS ==========
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PICKED_UP = "PICKED_UP";
    public static final String STATUS_IN_TRANSIT = "IN_TRANSIT";
    public static final String STATUS_DELIVERED = "DELIVERED";

    // ========== CREATE SHIPMENT ==========
    @Transactional
    public Shipment createShipment(Long orderId, CourierType courier, String receiverName, String deliveryAddress, Double shippingFee) {
        if (shipmentRepository.findByOrderId(orderId).isPresent()) {
            throw new RuntimeException("Shipment sudah ada untuk Order ID: " + orderId);
        }

        // Jika data penerima atau alamat kosong, ambil otomatis dari profil Customer
        if (receiverName == null || receiverName.trim().isEmpty() || deliveryAddress == null || deliveryAddress.trim().isEmpty()) {
            try {
                org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.set("Authorization", "Bearer " + jwtUtil.generateSystemToken());
                org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

                // 1. Dapatkan customerId dari Order Service
                String orderUrl = "http://localhost:8084/api/orders/" + orderId;
                org.springframework.http.ResponseEntity<java.util.Map> orderResponse = restTemplate.exchange(orderUrl, org.springframework.http.HttpMethod.GET, entity, java.util.Map.class);
                java.util.Map orderData = (java.util.Map) orderResponse.getBody().get("data");
                Long customerId = Long.valueOf(orderData.get("customerId").toString());

                // 2. Dapatkan Name & Address dari Customer Service
                String customerUrl = "http://localhost:8083/api/customers/" + customerId;
                org.springframework.http.ResponseEntity<java.util.Map> customerResponse = restTemplate.exchange(customerUrl, org.springframework.http.HttpMethod.GET, entity, java.util.Map.class);
                java.util.Map customerData = (java.util.Map) customerResponse.getBody().get("data");

                if (receiverName == null || receiverName.trim().isEmpty()) {
                    receiverName = customerData.get("name").toString();
                }
                if (deliveryAddress == null || deliveryAddress.trim().isEmpty()) {
                    deliveryAddress = customerData.get("address").toString();
                }
            } catch (Exception e) {
                System.err.println("Gagal mengambil data profil otomatis: " + e.getMessage());
                if (receiverName == null || deliveryAddress == null) {
                    throw new RuntimeException("Gagal mengambil data profil otomatis dan input manual kosong.");
                }
            }
        }

        Shipment shipment = new Shipment();
        shipment.setOrderId(orderId);
        shipment.setCourierName(courier); 
        shipment.setReceiverName(receiverName);
        shipment.setDeliveryAddress(deliveryAddress);
        shipment.setShippingFee(shippingFee);
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
        
        Shipment savedShipment = shipmentRepository.save(shipment);

        // --- MENGHUBUNGI ORDER SERVICE ---
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtUtil.generateSystemToken());

            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            restTemplate.exchange(
                "http://localhost:8084/api/orders/" + savedShipment.getOrderId() + "/shipping-status?status=" + newStatus,
                org.springframework.http.HttpMethod.PUT,
                entity,
                String.class
            );
        } catch (Exception e) {
            System.err.println("PERINGATAN: Gagal mengupdate status order di Order Service untuk Order ID " + savedShipment.getOrderId() + ". Error: " + e.getMessage());
        }

        return savedShipment;
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
