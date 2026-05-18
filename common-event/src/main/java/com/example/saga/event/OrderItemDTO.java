package com.example.saga.event;

public record OrderItemDTO(
        Long productId,
        Integer quantity
) {}
