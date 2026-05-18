package com.example.orderservice.kafka;

import com.example.saga.event.OrderCancelledEvent;
import com.example.saga.event.OrderCompletedEvent;
import com.example.saga.event.OrderCreatedEvent;
import com.example.saga.event.ReleaseProductReservationEvent;
import com.example.saga.event.RefundPaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderProducer.class);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void sendOrderCreated(OrderCreatedEvent event) {
        kafkaTemplate.send(KafkaTopics.ORDER_CREATED, event);
        log.info("[KAFKA PRODUCER] ORDER_CREATED               | orderId={}", event.orderId());
    }

    public void sendOrderCancelled(ReleaseProductReservationEvent event) {
        kafkaTemplate.send(KafkaTopics.RELEASE_PRODUCT_RESERVATION, event);
        log.info("[KAFKA PRODUCER] RELEASE_PRODUCT_RESERVATION | orderId={}", event.orderId());
    }

    public void sendRefundPayment(RefundPaymentEvent event) {
        kafkaTemplate.send(KafkaTopics.REFUND_PAYMENT, event);
        log.info("[KAFKA PRODUCER] REFUND_PAYMENT              | orderId={}", event.orderId());
    }

    /**
     * Publish OrderCancelledEvent ke topic order.cancelled.
     * Dikonsumsi oleh audit/notifikasi service — memberitahu bahwa order telah dibatalkan.
     */
    public void sendOrderCancelledEvent(OrderCancelledEvent event) {
        kafkaTemplate.send(KafkaTopics.ORDER_CANCELLED, event);
        log.info("[KAFKA PRODUCER] ORDER_CANCELLED             | orderId={} | reason={}",
            event.orderId(), event.reason());
    }

    /**
     * Publish OrderCompletedEvent ke topic order.completed.
     * Dikonsumsi oleh audit/analytics service — memberitahu bahwa seluruh Saga berhasil.
     */
    public void sendOrderCompleted(OrderCompletedEvent event) {
        kafkaTemplate.send(KafkaTopics.ORDER_COMPLETED, event);
        log.info("[KAFKA PRODUCER] ORDER_COMPLETED             | orderId={}", event.orderId());
    }
}