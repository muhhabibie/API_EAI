package com.example.paymentservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "saga_processed_events")
public class SagaProcessedEvent {

    @Id
    private String eventKey;

    private LocalDateTime processedAt;

    public SagaProcessedEvent() {
    }

    public SagaProcessedEvent(String eventKey) {
        this.eventKey = eventKey;
        this.processedAt = LocalDateTime.now();
    }

    public String getEventKey() {
        return eventKey;
    }

    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
