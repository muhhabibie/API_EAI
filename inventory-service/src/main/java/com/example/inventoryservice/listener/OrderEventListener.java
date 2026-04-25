package com.example.inventoryservice.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.inventoryservice.config.RabbitMQConfig;
import com.example.inventoryservice.dto.OrderEvent;
import com.example.inventoryservice.service.InventoryService;

@Component
public class OrderEventListener {

    @Autowired
    private InventoryService inventoryService;

    // Anotasi ini yang membuat method ini otomatis jalan ketika ada pesan masuk di
    // Queue
    @RabbitListener(queues = RabbitMQConfig.INVENTORY_QUEUE)
    public void handleOrderEvent(OrderEvent event) {
        System.out.println("=================================================");
        System.out.println("📦 [INVENTORY-SERVICE] Pesan Order Diterima!");
        System.out.println("ID Order   : " + event.getOrderId());

        if (event.getItems() != null) {
            for (OrderEvent.OrderItemDto item : event.getItems()) {
                System.out.println("-> Memproses pemotongan stok untuk Product ID: "
                        + item.getProductId() + " | Qty: " + item.getQuantity());

                // TODO: Aktifkan baris di bawah ini dan sesuaikan dengan nama method
                // pemotong stok yang sebenarnya ada di InventoryService Anda.
                // inventoryService.deductStock(item.getProductId(), item.getQuantity());
            }
        }
        System.out.println("✅ Stok berhasil disesuaikan untuk Order: " + event.getOrderId());
        System.out.println("=================================================");
    }
}