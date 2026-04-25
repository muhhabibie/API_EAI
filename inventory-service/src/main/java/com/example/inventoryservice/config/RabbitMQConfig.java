package com.example.inventoryservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "order-exchange";
    public static final String INVENTORY_QUEUE = "inventory-queue";
    public static final String ROUTING_KEY = "order.routing.key";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue inventoryQueue() {
        return new Queue(INVENTORY_QUEUE, true);
    }

    @Bean
    public Binding inventoryBinding(Queue inventoryQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(inventoryQueue).to(orderExchange).with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}