package com.example.order_management.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.order_management.entity.InventoryReservation;
import com.example.order_management.entity.Product;
import com.example.order_management.repository.InventoryReservationRepository;
import com.example.order_management.repository.ProductRepository;

@Service
public class InventoryService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    public int getStock(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return product.getStock();
    }

    @Transactional
    public InventoryReservation reserveStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        if (product.getStock() < quantity) {
            throw new RuntimeException("Insufficient stock");
        }
        // Kurangi stok sementara (opsional: bisa juga tidak dikurangi sampai confirm)
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);

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
        // Kembalikan stok
        Product product = productRepository.findById(reservation.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setStock(product.getStock() + reservation.getQuantity());
        productRepository.save(product);

        reservation.setStatus("RELEASED");
        reservationRepository.save(reservation);
    }
        @Transactional
    public void confirmReservation(Long productId, Integer quantity) {
        // Mencari reservasi yang masih ACTIVE untuk produk & jumlah tersebut
        // Dalam enterprise yang lebih kompleks, kita biasanya mencocokkan lewat reservationId
        InventoryReservation reservation = reservationRepository.findAll().stream()
            .filter(r -> r.getProductId().equals(productId) 
                    && r.getQuantity().equals(quantity) 
                    && "ACTIVE".equals(r.getStatus()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Data reservasi aktif tidak ditemukan untuk produk ID: " + productId));

        // Ubah status menjadi COMPLETED (Sah terjual)
        reservation.setStatus("COMPLETED");
        reservationRepository.save(reservation);
    }

    public java.util.List<InventoryReservation> getAllReservations() {
        return reservationRepository.findAll();
    }
}