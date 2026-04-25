package com.example.orderservice.kafka;

import com.example.orderservice.model.OrderMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderProducer {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "order-topic";

    public void sendOrderCreated(OrderMessage message) {
        kafkaTemplate.send(TOPIC, message);
        System.out.println("[KAFKA PRODUCER] Event terkirim: " + message.getOrderNumber());
    }
}