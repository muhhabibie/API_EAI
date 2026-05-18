package com.example.shippingservice.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shippingservice.entity.CourierType;
import com.example.shippingservice.entity.Shipment;
import com.example.shippingservice.repository.ShipmentRepository;

@Service
public class ShippingService {

    private static final Logger log = LoggerFactory.getLogger(ShippingService.class);

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private com.example.shippingservice.security.JwtUtil jwtUtil;

    @Autowired
    private org.springframework.web.client.RestTemplate restTemplate;

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

        // Dapatkan data order dari Order Service menggunakan System Token
        java.util.Map<String, Object> orderData;
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtUtil.generateSystemToken());
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            String orderUrl = "http://localhost:8084/api/orders/" + orderId;
            @SuppressWarnings("unchecked")
            org.springframework.http.ResponseEntity<java.util.Map<String, Object>> orderResponse = restTemplate.exchange(orderUrl, org.springframework.http.HttpMethod.GET, entity,
                    (Class<java.util.Map<String, Object>>) (Class<?>) java.util.Map.class);
            
            orderData = (java.util.Map<String, Object>) orderResponse.getBody().get("data");
        } catch (Exception e) {
            throw new RuntimeException("Gagal mengambil data pesanan. Pastikan Order ID " + orderId + " valid. Detail: " + e.getMessage());
        }

        // VALIDASI KRITIS: Pastikan pesanan sudah dibayar!
        String orderStatus = (String) orderData.get("status");
        if (!"PAID".equalsIgnoreCase(orderStatus)) {
            throw new RuntimeException("Tidak dapat membuat pengiriman! Pesanan belum lunas. Status pesanan saat ini: " + orderStatus);
        }

        Long customerId = Long.valueOf(orderData.get("customerId").toString());

        // Jika data penerima atau alamat kosong, ambil otomatis dari profil Customer
        if (receiverName == null || receiverName.trim().isEmpty() || deliveryAddress == null || deliveryAddress.trim().isEmpty()) {
            try {
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.set("Authorization", "Bearer " + jwtUtil.generateSystemToken());
                org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

                // 2. Dapatkan Name & Address dari Customer Service
                String customerUrl = "http://localhost:8083/api/customers/" + customerId;
                @SuppressWarnings("unchecked")
                org.springframework.http.ResponseEntity<java.util.Map<String, Object>> customerResponse = restTemplate.exchange(customerUrl, org.springframework.http.HttpMethod.GET, entity,
                        (Class<java.util.Map<String, Object>>) (Class<?>) java.util.Map.class);
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> customerData = (java.util.Map<String, Object>) customerResponse.getBody().get("data");

                if (receiverName == null || receiverName.trim().isEmpty()) {
                    Object nameObj = customerData.get("name");
                    receiverName = (nameObj != null) ? nameObj.toString() : "Penerima Tidak Diketahui";
                }
                if (deliveryAddress == null || deliveryAddress.trim().isEmpty()) {
                    // FIX #6: Null-safe — kolom address di Customer tidak wajib diisi
                    Object addrObj = customerData.get("address");
                    deliveryAddress = (addrObj != null && !addrObj.toString().isBlank())
                        ? addrObj.toString() : "Alamat belum diisi";
                }
            } catch (Exception e) {
                log.warn("[SHIPPING-SERVICE] Gagal ambil profil otomatis | orderId={} | alasan={}", orderId, e.getMessage());
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

        // --- MENGHUBUNGI ORDER SERVICE VIA KAFKA (EVENT-DRIVEN) ---
        try {
            if (STATUS_PICKED_UP.equals(newStatus) || STATUS_IN_TRANSIT.equals(newStatus)) {
                com.example.saga.event.OrderShippedEvent event = new com.example.saga.event.OrderShippedEvent(
                    String.valueOf(savedShipment.getOrderId()), savedShipment.getTrackingNumber()
                );
                kafkaTemplate.send(com.example.shippingservice.kafka.KafkaTopics.ORDER_SHIPPED, event);
                log.info("[SHIPPING-SAGA]  ► EVENT : ORDER_SHIPPED         | orderId={} | tracking={}", savedShipment.getOrderId(), savedShipment.getTrackingNumber());
            } else if (STATUS_DELIVERED.equals(newStatus)) {
                com.example.saga.event.OrderDeliveredEvent event = new com.example.saga.event.OrderDeliveredEvent(
                    String.valueOf(savedShipment.getOrderId())
                );
                kafkaTemplate.send(com.example.shippingservice.kafka.KafkaTopics.ORDER_DELIVERED, event);
                log.info("[SHIPPING-SAGA]  ► EVENT : ORDER_DELIVERED       | orderId={}", savedShipment.getOrderId());
            }
        } catch (Exception e) {
            log.warn("[SHIPPING-SERVICE] Gagal publish event shipping | orderId={} | alasan={}", savedShipment.getOrderId(), e.getMessage());
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
