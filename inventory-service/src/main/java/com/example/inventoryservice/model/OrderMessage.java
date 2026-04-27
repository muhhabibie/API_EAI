package com.example.inventoryservice.model;

public class OrderMessage {
    private Long id;
    private String orderNumber;
    private Long customerId;
    private String status;
    private Double totalAmount;
    private java.util.List<OrderItemMessage> items;

    public OrderMessage() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public java.util.List<OrderItemMessage> getItems() { return items; }
    public void setItems(java.util.List<OrderItemMessage> items) { this.items = items; }

    public static class OrderItemMessage {
        private Long productId;
        private Integer quantity;

        public OrderItemMessage() {}

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}