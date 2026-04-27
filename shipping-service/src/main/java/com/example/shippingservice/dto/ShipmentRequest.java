package com.example.shippingservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class ShipmentRequest {
    @Schema(example = "1", description = "ID dari Order yang akan dikirim")
    private Long orderId;
    
    @Schema(example = "JNE Express", description = "Nama kurir pengiriman")
    private String courierName;
    
    @Schema(example = "Muhammad Habibi", description = "Nama penerima paket")
    private String receiverName;
    
    @Schema(example = "Jl. Merdeka No. 10, Jakarta", description = "Alamat tujuan pengiriman")
    private String deliveryAddress;
    
    @Schema(example = "20000.0", description = "Biaya ongkos kirim")
    private Double shippingFee;

    // Getters and Setters
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getCourierName() { return courierName; }
    public void setCourierName(String courierName) { this.courierName = courierName; }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public Double getShippingFee() { return shippingFee; }
    public void setShippingFee(Double shippingFee) { this.shippingFee = shippingFee; }
}
