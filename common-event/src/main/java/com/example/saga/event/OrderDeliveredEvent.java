package com.example.saga.event;

public record OrderDeliveredEvent(
        String orderId
) {}
