package com.example.orderservice.kafka;

import com.example.orderservice.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class PaymentConsumer {

    @Autowired
    private OrderService orderService;

    @KafkaListener(topics = "payment-topic", groupId = "order-group")
    public void listenPayment(String message) {
        System.out.println("[KAFKA CONSUMER] Menerima sinyal pembayaran: " + message);
        
        try {
            // Format pesan: orderId:status
            String[] parts = message.split(":");
            Long orderId = Long.parseLong(parts[0]);
            String status = parts[1];

            if ("SUCCESS".equals(status)) {
                // Update order ke PAID dan trigger shipping
                orderService.confirmPayment(orderId, "JNE-REGULER");
                System.out.println("[KAFKA CONSUMER] Order " + orderId + " otomatis ditandai PAID dan masuk pengiriman.");
            } else if ("FAILED".equals(status)) {
                // Jangan batalkan order, beri kesempatan user coba lagi
                System.out.println("[KAFKA CONSUMER] Pembayaran untuk Order " + orderId + " gagal. Pesanan tetap PENDING agar user bisa mencoba lagi.");
            }
        } catch (Exception e) {
            System.err.println("Gagal memproses sinyal pembayaran: " + e.getMessage());
        }
    }
}
