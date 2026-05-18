package com.example.productservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.productservice.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * FIX #3: Sesuaikan stok secara atomik.
     * WHERE stock + amount >= 0 mencegah stok negatif meskipun ada concurrent requests.
     * Return 1 = sukses, 0 = stok tidak cukup atau produk tidak ditemukan.
     */
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stock = p.stock + :amount WHERE p.id = :id AND (p.stock + :amount) >= 0")
    int adjustStockAtomic(@Param("id") Long id, @Param("amount") int amount);
}
