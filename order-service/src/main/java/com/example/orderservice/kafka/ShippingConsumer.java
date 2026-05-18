package com.example.orderservice.kafka;

import com.example.orderservice.service.OrderService;
import com.example.saga.event.OrderShippedEvent;
import com.example.saga.event.OrderDeliveredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ShippingConsumer {

    private static final Logger log = LoggerFactory.getLogger(ShippingConsumer.class);

    @Autowired
    private OrderService orderService;

    @KafkaListener(topics = "order.shipped", groupId = "order-group")
    public void onOrderShipped(OrderShippedEvent event) {
        log.info("[ORDER-SAGA]    ► EVENT : ORDER_SHIPPED            | orderId={} | tracking={}", event.orderId(), event.trackingNumber());
        try {
            Long orderId = Long.parseLong(event.orderId());
            orderService.updateStatus(orderId, "SHIPPED");
            log.info("[ORDER-SAGA]    ✓ SUKSES : Order diupdate ke SHIPPED | orderId={}", orderId);
        } catch (Exception e) {
            log.error("[ORDER-SAGA]    ✗ GAGAL  : Update SHIPPED           | orderId={} | alasan={}", event.orderId(), e.getMessage());
        }
    }

    @KafkaListener(topics = "order.delivered", groupId = "order-group")
    public void onOrderDelivered(OrderDeliveredEvent event) {
        log.info("[ORDER-SAGA]    ► EVENT : ORDER_DELIVERED          | orderId={}", event.orderId());
        try {
            Long orderId = Long.parseLong(event.orderId());
            orderService.updateStatus(orderId, "DELIVERED");
            
            // Publish OrderCompletedEvent — menandakan seluruh alur Saga Happy Path berhasil
            orderService.publishOrderCompletedEvent(orderId);
            
            log.info("[ORDER-SAGA]    ✓ SUKSES : Order diupdate ke DELIVERED | orderId={}", orderId);
        } catch (Exception e) {
            log.error("[ORDER-SAGA]    ✗ GAGAL  : Update DELIVERED         | orderId={} | alasan={}", event.orderId(), e.getMessage());
        }
    }
}
