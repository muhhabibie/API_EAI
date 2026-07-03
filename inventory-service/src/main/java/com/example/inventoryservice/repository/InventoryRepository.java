package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    /**
     * Tambah/kurangi reservedQty secara atomik.
     * Hanya berhasil jika:
     *   - (reservedQty + delta) >= 0          → tidak bisa negatif
     *   - (totalQty - reservedQty - delta) >= 0 → availableQty tidak minus
     *
     * Return: jumlah baris yang diupdate (1 = sukses, 0 = stok tidak cukup)
     */
    @Modifying
    @Query("""
        UPDATE Inventory i
        SET i.reservedQty  = i.reservedQty + :delta,
            i.availableQty = i.totalQty - (i.reservedQty + :delta),
            i.updatedAt    = CURRENT_TIMESTAMP
        WHERE i.productId = :productId
          AND (i.reservedQty + :delta) >= 0
          AND (i.totalQty - i.reservedQty - :delta) >= 0
        """)
    int adjustReservedAtomic(@Param("productId") Long productId, @Param("delta") int delta);

    /**
     * Kurangi totalQty sekaligus reservedQty secara atomik (saat payment confirmed).
     * Hanya berhasil jika tidak membuat totalQty atau availableQty menjadi negatif.
     */
    @Modifying
    @Query("""
        UPDATE Inventory i
        SET i.totalQty     = i.totalQty - :qty,
            i.reservedQty  = i.reservedQty - :qty,
            i.availableQty = i.totalQty - i.reservedQty,
            i.updatedAt    = CURRENT_TIMESTAMP
        WHERE i.productId = :productId
          AND (i.totalQty - :qty) >= 0
          AND (i.reservedQty - :qty) >= 0
        """)
    int confirmAndDeductAtomic(@Param("productId") Long productId, @Param("qty") int qty);
}
