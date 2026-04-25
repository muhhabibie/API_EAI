package com.example.shippingservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "order-exchange";
    public static final String SHIPPING_QUEUE = "shipping-queue";
    public static final String ROUTING_KEY = "order.routing.key";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue shippingQueue() {
        return new Queue(SHIPPING_QUEUE, true);
    }

    @Bean
    public Binding shippingBinding(Queue shippingQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(shippingQueue).to(orderExchange).with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}