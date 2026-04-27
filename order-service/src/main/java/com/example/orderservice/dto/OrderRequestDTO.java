package com.example.orderservice.dto;

import java.util.List;
import com.example.orderservice.service.OrderService;

public class OrderRequestDTO {
    private Long customerId;
    private List<OrderService.ItemRequest> items;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public List<OrderService.ItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderService.ItemRequest> items) {
        this.items = items;
    }
}
