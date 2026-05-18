package com.example.orderservice.kafka;

import com.example.orderservice.service.IdempotencyService;
import com.example.orderservice.service.OrderService;
import com.example.saga.event.PaymentProcessedEvent;
import com.example.saga.event.PaymentFailedEvent;
import com.example.saga.event.ProductReservedEvent;
import com.example.saga.event.ProductReservationFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class PaymentConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentConsumer.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private IdempotencyService idempotencyService;

    // FIX #7: Kurir default tidak lagi hardcoded — bisa dikonfigurasi via application.properties
    @Value("${order.saga.default-courier:JNE-REGULER}")
    private String defaultCourier;

    @KafkaListener(topics = KafkaTopics.PRODUCT_RESERVED, groupId = "order-group")
    public void onProductReserved(ProductReservedEvent event) {
        String eventKey = "PRODUCT_RESERVED_ORDER_" + event.orderId();
        if (idempotencyService.isAlreadyProcessed(eventKey)) {
            log.warn("[ORDER-SAGA]    ⚠  DUPLIKAT diabaikan  | event={}", eventKey);
            return;
        }
        log.info("[ORDER-SAGA]    ► EVENT : PRODUCT_RESERVED         | orderId={}", event.orderId());
        try {
            Long orderId = Long.parseLong(event.orderId());
            orderService.updateStatus(orderId, "AWAITING_PAYMENT");
            log.info("[ORDER-SAGA]    ✓ SUKSES : Status diupdate          | orderId={} | status=PENDING → AWAITING_PAYMENT", orderId);
        } catch (Exception e) {
            log.error("[ORDER-SAGA]    ✗ GAGAL  : Update AWAITING_PAYMENT  | orderId={} | alasan={}", event.orderId(), e.getMessage());
        }
    }

    @KafkaListener(topics = KafkaTopics.PRODUCT_RESERVATION_FAILED, groupId = "order-group")
    public void onProductReservationFailed(ProductReservationFailedEvent event) {
        String eventKey = "RESERVATION_FAILED_" + event.orderId();
        if (idempotencyService.isAlreadyProcessed(eventKey)) {
            log.warn("[ORDER-SAGA]    ⚠  DUPLIKAT diabaikan  | event={}", eventKey);
            return;
        }
        log.warn("[ORDER-SAGA]    ► EVENT : PRODUCT_RESERVATION_FAILED | orderId={}", event.orderId());
        try {
            Long orderId = Long.parseLong(event.orderId());
            orderService.cancelOrder(orderId);
            log.warn("[ORDER-SAGA]    ↩ KOMPENSASI: Order di-CANCEL       | orderId={} | alasan=Stok habis/gagal", orderId);
        } catch (Exception e) {
            log.error("[ORDER-SAGA]    ✗ GAGAL  : Cancel order              | orderId={} | alasan={}", event.orderId(), e.getMessage());
        }
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_PROCESSED, groupId = "order-group")
    public void onPaymentProcessed(PaymentProcessedEvent event) {
        String eventKey = "PAYMENT_PROCESSED_ORDER_" + event.orderId();
        if (idempotencyService.isAlreadyProcessed(eventKey)) {
            log.warn("[ORDER-SAGA]    ⚠  DUPLIKAT diabaikan  | event={}", eventKey);
            return;
        }
        log.info("[ORDER-SAGA]    ► EVENT : PAYMENT_PROCESSED         | orderId={} | ref={}", event.orderId(), event.reference());
        try {
            Long orderId = Long.parseLong(event.orderId());
            orderService.confirmPayment(orderId, defaultCourier);
            log.info("[ORDER-SAGA]    ✓ SUKSES : Order dikonfirmasi        | orderId={} | status=AWAITING_PAYMENT → PAID", orderId);
        } catch (Exception e) {
            log.error("[ORDER-SAGA]    ✗ GAGAL  : Konfirmasi payment        | orderId={} | alasan={}", event.orderId(), e.getMessage());
        }
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_FAILED, groupId = "order-group")
    public void onPaymentFailed(PaymentFailedEvent event) {
        String eventKey = "PAYMENT_FAILED_" + event.orderId();
        if (idempotencyService.isAlreadyProcessed(eventKey)) {
            log.warn("[ORDER-SAGA]    ⚠  DUPLIKAT diabaikan  | event={}", eventKey);
            return;
        }
        log.warn("[ORDER-SAGA]    ► EVENT : PAYMENT_FAILED            | orderId={}", event.orderId());
        try {
            Long orderId = Long.parseLong(event.orderId());
            orderService.cancelOrder(orderId);
            log.warn("[ORDER-SAGA]    ↩ KOMPENSASI: Order di-CANCEL       | orderId={} | alasan=Saldo tidak cukup", orderId);
        } catch (Exception e) {
            log.error("[ORDER-SAGA]    ✗ GAGAL  : Cancel order              | orderId={} | alasan={}", event.orderId(), e.getMessage());
        }
    }
}
