package com.example.inventoryservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.inventoryservice.entity.InventoryReservation;
import com.example.inventoryservice.repository.InventoryReservationRepository;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @Autowired
    private com.example.inventoryservice.security.JwtUtil jwtUtil;

    @Autowired
    private org.springframework.web.client.RestTemplate restTemplate;

    @Transactional
    public InventoryReservation reserveStock(String orderNumber, Long productId, int quantity) {
        // FIX #2: Idempotency via UNIQUE constraint di DB + tangkap DataIntegrityViolationException.
        // Pattern lama (existsBy + save terpisah) rentan race condition jika 2 thread masuk bersamaan.
        // Sekarang: coba INSERT langsung — jika duplikat, DB akan tolak dengan constraint violation.
        try {
            // Potong stok aktual di Product Service menggunakan Internal Token
            try {
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.set("Authorization", "Bearer " + jwtUtil.generateSystemToken());
                org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(null, headers);

                org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    "http://localhost:8082/api/products/" + productId + "/adjustment?amount=-" + quantity,
                    org.springframework.http.HttpMethod.POST,
                    entity,
                    String.class
                );

                if (!response.getStatusCode().is2xxSuccessful()) {
                    throw new RuntimeException("Gagal memotong stok dari database.");
                }
            } catch (org.springframework.web.client.HttpClientErrorException.BadRequest e) {
                throw new RuntimeException("Stok tidak mencukupi untuk Produk ID: " + productId);
            } catch (Exception e) {
                throw new RuntimeException("Gagal terhubung ke Product Service: " + e.getMessage());
            }

            InventoryReservation reservation = new InventoryReservation(orderNumber, productId, quantity);
            return reservationRepository.save(reservation);

        } catch (DataIntegrityViolationException e) {
            // UNIQUE constraint (order_number, product_id) dilanggar — ini duplikat event
            log.warn("[INVENTORY-SAGA] ⚠  DUPLIKAT diabaikan (constraint) | orderNumber={} | productId={}", orderNumber, productId);
            return null;
        }
    }

    @Transactional
    public void releaseReservationByOrderNumber(String orderNumber) {
        java.util.List<InventoryReservation> reservations = reservationRepository.findByOrderNumber(orderNumber);
        if (reservations.isEmpty()) {
            throw new RuntimeException("Reservation not found for order: " + orderNumber);
        }

        for (InventoryReservation reservation : reservations) {
            if ("ACTIVE".equals(reservation.getStatus()) || "COMPLETED".equals(reservation.getStatus())) {
                reservation.setStatus("RELEASED");
                reservationRepository.save(reservation);

                // Kembalikan stok aktual ke Product Service
                try {
                    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                    headers.set("Authorization", "Bearer " + jwtUtil.generateSystemToken());
                    org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(null, headers);

                    restTemplate.exchange(
                        "http://localhost:8082/api/products/" + reservation.getProductId() + "/adjustment?amount=" + reservation.getQuantity(),
                        org.springframework.http.HttpMethod.POST,
                        entity,
                        String.class
                    );
                } catch (Exception e) {
                    log.error("[INVENTORY-SAGA] ✗ Gagal kembalikan stok ke Product Service | productId={} | alasan={}",
                        reservation.getProductId(), e.getMessage());
                }
            }
        }
    }

    @Transactional
    public void confirmReservationByOrderNumber(String orderNumber) {
        java.util.List<InventoryReservation> reservations = reservationRepository.findByOrderNumber(orderNumber);
        if (reservations.isEmpty()) {
            throw new RuntimeException("Data reservasi aktif tidak ditemukan untuk order: " + orderNumber);
        }

        for (InventoryReservation reservation : reservations) {
            if ("ACTIVE".equals(reservation.getStatus())) {
                reservation.setStatus("COMPLETED");
                reservationRepository.save(reservation);
            }
        }
    }

    public java.util.List<InventoryReservation> getAllReservations() {
        return reservationRepository.findAll();
    }
}

