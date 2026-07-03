package com.example.inventoryservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.entity.InventoryReservation;
import com.example.inventoryservice.repository.InventoryRepository;
import com.example.inventoryservice.repository.InventoryReservationRepository;

import java.util.List;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    // ── Sync dari Product Service ──────────────────────────────────────────────

    /**
     * Dipanggil saat menerima event product.stock.synced dari Product Service.
     * Upsert tabel inventory: jika belum ada untuk productId, buat baru.
     * Jika sudah ada, update totalQty dan recalc availableQty.
     */
    @Transactional
    public void syncFromProduct(Long productId, int newStock) {
        Inventory inv = inventoryRepository.findByProductId(productId)
                .orElseGet(() -> new Inventory(productId, 0));

        int oldTotal = inv.getTotalQty();
        inv.setTotalQty(newStock);
        // availableQty = totalQty - reservedQty (reservedQty tidak berubah)
        inv.recalcAvailable();
        inventoryRepository.save(inv);

        log.info("[INVENTORY-STOCK] ✓ SYNC dari Product | productId={} | totalQty: {} → {}", productId, oldTotal, newStock);
    }

    // ── Reserve (order.created) ────────────────────────────────────────────────

    /**
     * Reservasi stok dari tabel inventory lokal (tanpa REST ke Product Service).
     * Menggunakan query atomik untuk mencegah oversell meski ada concurrent request.
     */
    @Transactional
    public InventoryReservation reserveStock(String orderNumber, Long productId, int quantity) {
        try {
            // Cek dulu apakah data inventory sudah ada untuk produk ini
            Inventory inv = inventoryRepository.findByProductId(productId)
                    .orElseGet(() -> {
                        log.warn("[INVENTORY-STOCK] ⚠ Data inventory untuk Produk ID {} tidak ditemukan. Mengambil dari Product Service...", productId);
                        try {
                            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                            String productUrl = "http://localhost:8082/api/products/" + productId;
                            
                            @SuppressWarnings("unchecked")
                            java.util.Map<String, Object> response = restTemplate.getForObject(productUrl, java.util.Map.class);
                            if (response != null && response.get("data") != null) {
                                @SuppressWarnings("unchecked")
                                java.util.Map<String, Object> productData = (java.util.Map<String, Object>) response.get("data");
                                Object stockObj = productData.get("stock");
                                if (stockObj != null) {
                                    int stock = Integer.parseInt(stockObj.toString());
                                    Inventory newInv = new Inventory(productId, stock);
                                    newInv.recalcAvailable();
                                    Inventory savedInv = inventoryRepository.save(newInv);
                                    log.info("[INVENTORY-STOCK] ✓ Berhasil sinkronisasi on-demand untuk Produk ID {} dengan stock {}", productId, stock);
                                    return savedInv;
                                }
                            }
                        } catch (Exception ex) {
                            log.error("[INVENTORY-STOCK] ✗ GAGAL mengambil stock dari Product Service untuk ID {}: {}", productId, ex.getMessage());
                        }
                        throw new RuntimeException("Data inventory untuk Produk ID " + productId + " belum tersinkronisasi.");
                    });

            log.info("[INVENTORY-STOCK] ► Reservasi | productId={} | qty={} | available={}", productId, quantity, inv.getAvailableQty());

            // Kurangi availableQty secara atomik (mencegah race condition)
            int updated = inventoryRepository.adjustReservedAtomic(productId, quantity);
            if (updated == 0) {
                throw new RuntimeException("Stok tidak mencukupi untuk Produk ID: " + productId
                        + " (tersedia=" + inv.getAvailableQty() + ", diminta=" + quantity + ")");
            }

            // Simpan catatan reservasi per-order
            InventoryReservation reservation = new InventoryReservation(orderNumber, productId, quantity);
            return reservationRepository.save(reservation);

        } catch (DataIntegrityViolationException e) {
            log.warn("[INVENTORY-SAGA] ⚠ DUPLIKAT diabaikan | orderNumber={} | productId={}", orderNumber, productId);
            return null;
        }
    }

    // ── Release (order cancelled / payment failed) ─────────────────────────────

    /**
     * Lepas reservasi stok — kembalikan qty ke availableQty di tabel inventory.
     * Product Service stock TIDAK berubah (stok fisik belum berkurang).
     */
    @Transactional
    public void releaseReservationByOrderNumber(String orderNumber) {
        List<InventoryReservation> reservations = reservationRepository.findByOrderNumber(orderNumber);
        if (reservations.isEmpty()) {
            throw new RuntimeException("Reservation not found for order: " + orderNumber);
        }

        for (InventoryReservation reservation : reservations) {
            if ("ACTIVE".equals(reservation.getStatus())) {
                // Kembalikan reservedQty (kurangi reserved → naikkan available)
                int updated = inventoryRepository.adjustReservedAtomic(reservation.getProductId(), -reservation.getQuantity());
                if (updated == 0) {
                    log.warn("[INVENTORY-STOCK] ⚠ adjustReserved gagal saat release | productId={}", reservation.getProductId());
                }

                reservation.setStatus("RELEASED");
                reservationRepository.save(reservation);
                log.info("[INVENTORY-STOCK] ✓ RELEASED | orderNumber={} | productId={} | qty={}", orderNumber, reservation.getProductId(), reservation.getQuantity());
            }
        }
    }

    // ── Confirm (payment.processed) ────────────────────────────────────────────

    /**
     * Konfirmasi reservasi setelah pembayaran berhasil:
     * - totalQty berkurang (stok fisik sudah terjual)
     * - reservedQty berkurang (reservasi selesai)
     * - availableQty tidak berubah (sudah dikurangi saat reserve)
     *
     * Mengembalikan total qty terkonfirmasi per productId untuk dipublish ke Product Service.
     */
    @Transactional
    public java.util.Map<Long, Integer> confirmReservationByOrderNumber(String orderNumber) {
        List<InventoryReservation> reservations = reservationRepository.findByOrderNumber(orderNumber);
        if (reservations.isEmpty()) {
            throw new RuntimeException("Data reservasi aktif tidak ditemukan untuk order: " + orderNumber);
        }

        java.util.Map<Long, Integer> confirmedQtyByProduct = new java.util.HashMap<>();

        for (InventoryReservation reservation : reservations) {
            if ("ACTIVE".equals(reservation.getStatus())) {
                int updated = inventoryRepository.confirmAndDeductAtomic(reservation.getProductId(), reservation.getQuantity());
                if (updated == 0) {
                    log.warn("[INVENTORY-STOCK] ⚠ confirmAndDeduct gagal | productId={}", reservation.getProductId());
                } else {
                    confirmedQtyByProduct.merge(reservation.getProductId(), reservation.getQuantity(), Integer::sum);
                    log.info("[INVENTORY-STOCK] ✓ CONFIRMED | orderNumber={} | productId={} | qty={}",
                            orderNumber, reservation.getProductId(), reservation.getQuantity());
                }

                reservation.setStatus("COMPLETED");
                reservationRepository.save(reservation);
            }
        }

        return confirmedQtyByProduct;
    }

    // ── Query ──────────────────────────────────────────────────────────────────

    public List<InventoryReservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    public List<Inventory> getAllInventory() {
        return inventoryRepository.findAll();
    }

    public Inventory getInventoryByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Data inventory untuk Produk ID " + productId + " tidak ditemukan"));
    }
}
