package com.example.inventoryservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.inventoryservice.entity.InventoryReservation;

import java.util.List;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {
    boolean existsByOrderNumberAndProductId(String orderNumber, Long productId);
    List<InventoryReservation> findByOrderNumber(String orderNumber);
}
