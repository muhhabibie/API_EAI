package com.example.inventoryservice.kafka;

import com.example.inventoryservice.model.OrderMessage;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderConsumer {

    @KafkaListener(topics = "order-topic", groupId = "inventory-group")
    public void consume(OrderMessage order) {
        System.out.println("[KAFKA CONSUMER] Menerima order: " + order.getOrderNumber());
        System.out.println("Customer ID: " + order.getCustomerId());
        System.out.println("Total     : " + order.getTotalAmount());
    }
}