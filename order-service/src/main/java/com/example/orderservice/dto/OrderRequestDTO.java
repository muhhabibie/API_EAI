package com.example.orderservice.dto;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

public class OrderRequestDTO {
    @Schema(example = "1", description = "ID Customer yang memesan")
    private Long customerId;
    
    private List<OrderItemRequest> items;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }

    public static class OrderItemRequest {
        @Schema(example = "1", description = "ID Produk")
        private Long productId;
        
        @Schema(example = "2", description = "Jumlah yang dibeli")
        private Integer quantity;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
