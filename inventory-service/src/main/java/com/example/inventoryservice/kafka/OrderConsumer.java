package com.example.inventoryservice.kafka;

import com.example.inventoryservice.model.OrderMessage;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderConsumer {

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.inventoryservice.service.InventoryService inventoryService;

    @KafkaListener(topics = "order-topic", groupId = "inventory-group")
    public void consume(OrderMessage order) {
        System.out.println("[KAFKA CONSUMER] Menerima order baru: " + order.getOrderNumber() + " (Status: " + order.getStatus() + ")");
        
        if ("PENDING".equals(order.getStatus()) && order.getItems() != null) {
            System.out.println("-> Memproses pengurangan stok/reservasi untuk " + order.getItems().size() + " item...");
            for (OrderMessage.OrderItemMessage item : order.getItems()) {
                try {
                    inventoryService.reserveStock(order.getOrderNumber(), item.getProductId(), item.getQuantity());
                    System.out.println("   [SUCCESS] Reservasi berhasil untuk Product ID " + item.getProductId() + " sebanyak " + item.getQuantity());
                } catch (Exception e) {
                    System.err.println("   [FAILED] Gagal reservasi stok Product ID " + item.getProductId() + ": " + e.getMessage());
                }
            }
        } else if ("CANCELLED".equals(order.getStatus())) {
            System.out.println("-> Memproses pembatalan reservasi/pengembalian stok untuk Order: " + order.getOrderNumber());
            try {
                inventoryService.releaseReservationByOrderNumber(order.getOrderNumber());
                System.out.println("   [SUCCESS] Stok berhasil dikembalikan untuk Order: " + order.getOrderNumber());
            } catch (Exception e) {
                System.err.println("   [FAILED] Gagal mengembalikan stok: " + e.getMessage());
            }
        }
    }
}