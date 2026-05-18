package com.example.saga.event;

public record OrderShippedEvent(
        String orderId,
        String trackingNumber
) {}
