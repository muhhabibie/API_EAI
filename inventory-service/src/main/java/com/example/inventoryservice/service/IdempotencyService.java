package com.example.inventoryservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class IdempotencyService {

    @PersistenceContext
    private EntityManager entityManager;

    // initTable dihapus karena Spring Data JPA (hibernate.ddl-auto=update) 
    // akan otomatis membuat tabel berdasarkan entity SagaProcessedEvent.

    /**
     * Mengecek apakah sebuah event (berdasarkan eventKey unik) sudah pernah diproses.
     * Jika belum, catat ke database.
     * Menggunakan Propagation.REQUIRES_NEW agar ter-commit langsung ke database
     * sebelum proses bisnis utama selesai (untuk menghindari race condition).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean isAlreadyProcessed(String eventKey) {
        com.example.inventoryservice.entity.SagaProcessedEvent existing = 
            entityManager.find(com.example.inventoryservice.entity.SagaProcessedEvent.class, eventKey);

        if (existing != null) {
            return true; // Sudah pernah diproses (Duplikat)
        }

        com.example.inventoryservice.entity.SagaProcessedEvent newEvent = 
            new com.example.inventoryservice.entity.SagaProcessedEvent(eventKey);
        entityManager.persist(newEvent);

        return false; // Lolos, silakan diproses
    }
}
