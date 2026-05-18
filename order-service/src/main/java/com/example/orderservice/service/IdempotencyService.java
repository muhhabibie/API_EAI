package com.example.orderservice.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mencegah event Kafka yang sama diproses lebih dari satu kali.
 * Menyimpan eventKey unik ke tabel saga_processed_events.
 * Menggunakan REQUIRES_NEW agar INSERT langsung di-commit ke DB
 * sebelum proses bisnis utama selesai — mencegah race condition.
 */
@Service
public class IdempotencyService {

    @PersistenceContext
    private EntityManager entityManager;

    // initTable dihapus karena Spring Data JPA (hibernate.ddl-auto=update) 
    // akan otomatis membuat tabel berdasarkan entity SagaProcessedEvent.

    /**
     * @return true  → event sudah pernah diproses, ABAIKAN
     *         false → event baru, LANJUTKAN proses
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean isAlreadyProcessed(String eventKey) {
        com.example.orderservice.entity.SagaProcessedEvent existing = 
            entityManager.find(com.example.orderservice.entity.SagaProcessedEvent.class, eventKey);

        if (existing != null) {
            return true;
        }

        com.example.orderservice.entity.SagaProcessedEvent newEvent = 
            new com.example.orderservice.entity.SagaProcessedEvent(eventKey);
        entityManager.persist(newEvent);

        return false;
    }
}
