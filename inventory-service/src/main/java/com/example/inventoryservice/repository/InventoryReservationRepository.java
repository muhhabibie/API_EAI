package com.example.inventoryservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.inventoryservice.entity.InventoryReservation;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {
}
