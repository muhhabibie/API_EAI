package com.example.orderservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(
    name = "OrderRequest",
    description = "Body request untuk membuat order baru. Sertakan ID customer dan daftar produk beserta jumlahnya. " +
                  "Setelah order dibuat, sistem Saga akan otomatis memproses reservasi stok dan pembayaran."
)
public class OrderRequestDTO {

    @Schema(
        description = "ID Customer yang melakukan pemesanan.",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long customerId;

    @Schema(
        description = "Daftar produk yang dipesan. Minimal 1 item.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<OrderItemRequest> items;

    @Schema(description = "Nama Kurir yang dipilih.", example = "JNE")
    private String courierName;

    @Schema(description = "Ongkos Kirim.", example = "15000")
    private Double shippingFee;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }
    public String getCourierName() { return courierName; }
    public void setCourierName(String courierName) { this.courierName = courierName; }
    public Double getShippingFee() { return shippingFee; }
    public void setShippingFee(Double shippingFee) { this.shippingFee = shippingFee; }

    @Schema(name = "OrderItemRequest", description = "Detail satu item produk dalam order.")
    public static class OrderItemRequest {

        @Schema(description = "ID Produk yang dipesan.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long productId;

        @Schema(description = "Jumlah unit yang dibeli. Minimal 1.", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer quantity;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
