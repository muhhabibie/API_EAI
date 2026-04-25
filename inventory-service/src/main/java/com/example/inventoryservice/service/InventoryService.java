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

    @Transactional
    public InventoryReservation reserveStock(Long productId, int quantity) {
        InventoryReservation reservation = new InventoryReservation(productId, quantity);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public void releaseReservation(Long reservationId) {
        InventoryReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
        if (!"ACTIVE".equals(reservation.getStatus())) {
            throw new RuntimeException("Reservation already " + reservation.getStatus());
        }

        reservation.setStatus("RELEASED");
        reservationRepository.save(reservation);
    }

    @Transactional
    public void confirmReservation(Long productId, Integer quantity) {
        InventoryReservation reservation = reservationRepository.findAll().stream()
            .filter(r -> r.getProductId().equals(productId) 
                    && r.getQuantity().equals(quantity) 
                    && "ACTIVE".equals(r.getStatus()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Data reservasi aktif tidak ditemukan untuk produk ID: " + productId));

        reservation.setStatus("COMPLETED");
        reservationRepository.save(reservation);
    }

    public java.util.List<InventoryReservation> getAllReservations() {
        return reservationRepository.findAll();
    }
}
