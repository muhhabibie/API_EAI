package com.example.shippingservice.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.shippingservice.config.RabbitMQConfig;
import com.example.shippingservice.dto.OrderEvent;
import com.example.shippingservice.service.ShippingService;

@Component
public class OrderEventListener {

    @Autowired
    private ShippingService shippingService;

    @RabbitListener(queues = RabbitMQConfig.SHIPPING_QUEUE)
    public void handleOrderEvent(OrderEvent event) {
        System.out.println("=================================================");
        System.out.println("🚚 [SHIPPING-SERVICE] Pesan Order Diterima!");
        System.out.println("ID Order   : " + event.getOrderId());
        System.out.println("Kurir      : " + event.getCourierName());

        // TODO: Aktifkan baris di bawah ini dan sesuaikan dengan nama method
        // pembuatan resi pengiriman yang sebenarnya ada di ShippingService Anda.
        // shippingService.createShipment(event.getOrderId(), event.getCourierName());

        System.out.println("✅ Record pengiriman berhasil dibuat untuk Order: " + event.getOrderId());
        System.out.println("=================================================");
    }
}