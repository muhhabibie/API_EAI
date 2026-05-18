package com.example.inventoryservice.kafka;

import com.example.saga.event.OrderCreatedEvent;
import com.example.saga.event.OrderItemDTO;
import com.example.saga.event.ProductReservedEvent;
import com.example.saga.event.ProductReservationFailedEvent;
import com.example.saga.event.ReleaseProductReservationEvent;
import com.example.saga.event.PaymentProcessedEvent;
import com.example.inventoryservice.service.IdempotencyService;
import com.example.inventoryservice.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;


    // FLAG SIMULASI DLQ — Ubah ke TRUE untuk demo, FALSE untuk production
    
    private static final boolean SIMULATE_DLQ_ERROR = false;

    @KafkaListener(topics = KafkaTopics.ORDER_CREATED, groupId = "inventory-group")
    public void onOrderCreated(OrderCreatedEvent event) {
        if (SIMULATE_DLQ_ERROR) {
            log.warn("[INVENTORY-DLQ]   SIMULASI DLQ: melempar error ");
            throw new RuntimeException("[SIMULASI] Koneksi Database terputus / error kritis.");
        }
        String eventKey = "ORDER_CREATED_" + event.orderId();
        if (idempotencyService.isAlreadyProcessed(eventKey)) {
            log.warn("[INVENTORY-SAGA]  DUPLIKAT diabaikan  | event={}", eventKey);
            return;
        }

        log.info("[INVENTORY-SAGA] ► EVENT : ORDER_CREATED         | orderId={} | items={} | amount=Rp{}",
                event.orderId(), event.items().size(), event.amount());
        try {
            for (OrderItemDTO item : event.items()) {
                inventoryService.reserveStock(event.orderId(), item.productId(), item.quantity());
                log.info("[INVENTORY-SAGA]     stok OK | productId={} | qty={}", item.productId(), item.quantity());
            }
            ProductReservedEvent successEvent = new ProductReservedEvent(
                    event.orderId(), event.customerId(), event.items(), event.amount());
            kafkaTemplate.send(KafkaTopics.PRODUCT_RESERVED, successEvent);
            log.info("[INVENTORY-SAGA] ✓ SUKSES : Semua stok direservasi | orderId={} → publish PRODUCT_RESERVED", event.orderId());

        } catch (Exception e) {
            log.error("[INVENTORY-SAGA] ✗ GAGAL  : Reservasi stok        | orderId={} | alasan={}", event.orderId(), e.getMessage());
            try {
                inventoryService.releaseReservationByOrderNumber(event.orderId());
                log.warn("[INVENTORY-SAGA] ↩ KOMPENSASI: Partial rollback OK | orderId={}", event.orderId());
            } catch (Exception rollbackEx) {
                log.error("[INVENTORY-SAGA] ✗ KOMPENSASI GAGAL: rollback   | orderId={} | alasan={}", event.orderId(), rollbackEx.getMessage());
            }
            ProductReservationFailedEvent failedEvent = new ProductReservationFailedEvent(
                    event.orderId(), event.items(), e.getMessage());
            kafkaTemplate.send(KafkaTopics.PRODUCT_RESERVATION_FAILED, failedEvent);
            log.warn("[INVENTORY-SAGA] ↩ publish PRODUCT_RESERVATION_FAILED | orderId={}", event.orderId());
        }
    }

    @KafkaListener(topics = KafkaTopics.RELEASE_PRODUCT_RESERVATION, groupId = "inventory-group")
    public void onReleaseProductReservation(ReleaseProductReservationEvent event) {
        String eventKey = "RELEASE_RESERVATION_" + event.orderId();
        if (idempotencyService.isAlreadyProcessed(eventKey)) {
            log.warn("[INVENTORY-SAGA] ⚠  DUPLIKAT diabaikan  | event={}", eventKey);
            return;
        }

        log.info("[INVENTORY-SAGA] ► EVENT : RELEASE_RESERVATION    | orderId={} | alasan={}", event.orderId(), event.reason());
        try {
            inventoryService.releaseReservationByOrderNumber(event.orderId());
            log.info("[INVENTORY-SAGA] ✓ SUKSES : Stok dikembalikan     | orderId={} | status=RELEASED", event.orderId());
        } catch (Exception e) {
            log.error("[INVENTORY-SAGA] ✗ GAGAL  : Kembalikan stok       | orderId={} | alasan={}", event.orderId(), e.getMessage());
        }
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_PROCESSED, groupId = "inventory-group")
    public void onPaymentProcessed(PaymentProcessedEvent event) {
        String eventKey = "PAYMENT_PROCESSED_" + event.orderId();
        if (idempotencyService.isAlreadyProcessed(eventKey)) {
            log.warn("[INVENTORY-SAGA] ⚠  DUPLIKAT diabaikan  | event={}", eventKey);
            return;
        }

        log.info("[INVENTORY-SAGA] ► EVENT : PAYMENT_PROCESSED       | orderId={} | trxId={}", event.orderId(), event.reference());
        try {
            inventoryService.confirmReservationByOrderNumber(event.orderId());
            log.info("[INVENTORY-SAGA] ✓ SUKSES : Reservasi dikonfirmasi | orderId={} | status=COMPLETED", event.orderId());
        } catch (Exception e) {
            log.error("[INVENTORY-SAGA] ✗ GAGAL  : Konfirmasi reservasi   | orderId={} | alasan={}", event.orderId(), e.getMessage());
        }
    }
}