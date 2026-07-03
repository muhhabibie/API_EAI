package com.example.inventoryservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Tabel ringkasan stok per produk di Inventory Service.
 *
 * - totalQty     : stok fisik sesungguhnya (sinkron dengan product.stock)
 * - reservedQty  : stok yang sedang dikunci oleh order aktif (PENDING/AWAITING_PAYMENT)
 * - availableQty : stok yang masih bisa dipesan = totalQty - reservedQty
 *
 * Sinkronisasi:
 * - totalQty diperbarui dari Product Service via event product.stock.synced
 * - reservedQty diperbarui saat order dibuat / dibatalkan / pembayaran dikonfirmasi
 */
@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, name = "product_id")
    private Long productId;

    @Column(nullable = false, name = "total_qty")
    private int totalQty;

    @Column(nullable = false, name = "reserved_qty")
    private int reservedQty;

    @Column(nullable = false, name = "available_qty")
    private int availableQty;

    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;

    public Inventory() {}

    public Inventory(Long productId, int totalQty) {
        this.productId    = productId;
        this.totalQty     = totalQty;
        this.reservedQty  = 0;
        this.availableQty = totalQty;
        this.updatedAt    = LocalDateTime.now();
    }

    // ── getters & setters ─────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public int getTotalQty() { return totalQty; }
    public void setTotalQty(int totalQty) { this.totalQty = totalQty; }

    public int getReservedQty() { return reservedQty; }
    public void setReservedQty(int reservedQty) { this.reservedQty = reservedQty; }

    public int getAvailableQty() { return availableQty; }
    public void setAvailableQty(int availableQty) { this.availableQty = availableQty; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ── helper ────────────────────────────────────────────────────

    /** Recalculate availableQty berdasarkan totalQty dan reservedQty */
    public void recalcAvailable() {
        this.availableQty = this.totalQty - this.reservedQty;
        this.updatedAt    = LocalDateTime.now();
    }
}
