package com.example.inventoryservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.inventoryservice.entity.InventoryReservation;
import com.example.inventoryservice.repository.InventoryReservationRepository;

@Service
public class InventoryService {

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @Autowired
    private com.example.inventoryservice.security.JwtUtil jwtUtil;

    @Transactional
    public InventoryReservation reserveStock(String orderNumber, Long productId, int quantity) {
        // Cek Idempotency: Jika sudah pernah dipotong stoknya untuk order ini, abaikan!
        if (reservationRepository.existsByOrderNumberAndProductId(orderNumber, productId)) {
            System.out.println("Idempotency Terjaga: Order " + orderNumber + " untuk produk " + productId + " sudah direservasi sebelumnya.");
            return null; // Atau throw custom exception jika diperlukan
        }

        // --- MENGHINDARI PHANTOM STOCK ---
        // Potong stok aktual di Product Service menggunakan Internal Token
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtUtil.generateSystemToken());
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(null, headers);

            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:8082/api/products/" + productId + "/adjustment?amount=-" + quantity,
                org.springframework.http.HttpMethod.PATCH,
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
    }

    @Transactional
    public void releaseReservationByOrderNumber(String orderNumber) {
        java.util.List<InventoryReservation> reservations = reservationRepository.findByOrderNumber(orderNumber);
        if (reservations.isEmpty()) {
            throw new RuntimeException("Reservation not found for order: " + orderNumber);
        }

        for (InventoryReservation reservation : reservations) {
            if ("ACTIVE".equals(reservation.getStatus())) {
                reservation.setStatus("RELEASED");
                reservationRepository.save(reservation);

                // --- MENGEMBALIKAN STOK ASLI ---
                try {
                    org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                    headers.set("Authorization", "Bearer " + jwtUtil.generateSystemToken());
                    org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(null, headers);

                    restTemplate.exchange(
                        "http://localhost:8082/api/products/" + reservation.getProductId() + "/adjustment?amount=" + reservation.getQuantity(),
                        org.springframework.http.HttpMethod.PATCH,
                        entity,
                        String.class
                    );
                } catch (Exception e) {
                    System.err.println("Gagal mengembalikan stok aktual ke Product Service untuk Produk ID: " + reservation.getProductId());
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

    @Transactional
    public void releaseReservation(Long id) {
        InventoryReservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservasi tidak ditemukan"));

        if ("ACTIVE".equals(reservation.getStatus())) {
            reservation.setStatus("RELEASED");
            reservationRepository.save(reservation);

            // --- MENGEMBALIKAN STOK ASLI ---
            try {
                org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.set("Authorization", "Bearer " + jwtUtil.generateSystemToken());
                org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(null, headers);

                restTemplate.exchange(
                    "http://localhost:8082/api/products/" + reservation.getProductId() + "/adjustment?amount=" + reservation.getQuantity(),
                    org.springframework.http.HttpMethod.PATCH,
                    entity,
                    String.class
                );
            } catch (Exception e) {
                System.err.println("Gagal mengembalikan stok aktual ke Product Service untuk Produk ID: " + reservation.getProductId());
            }
        }
    }
}
