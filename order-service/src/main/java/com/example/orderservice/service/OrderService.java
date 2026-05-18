package com.example.orderservice.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.kafka.OrderProducer;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.dto.OrderRequestDTO;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderProducer orderProducer;

    @Autowired
    private org.springframework.web.client.RestTemplate restTemplate;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_AWAITING_PAYMENT = "AWAITING_PAYMENT";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_SHIPPED = "SHIPPED";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @Transactional
    public Order createOrder(Long customerId, List<OrderRequestDTO.OrderItemRequest> itemRequests) {
        if (customerId == null) {
            throw new RuntimeException("Customer ID tidak boleh kosong");
        }
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new RuntimeException("Item order tidak boleh kosong");
        }

        Order order = new Order();
        order.setCustomerId(customerId);
        order.setOrderNumber("ORD-" + System.currentTimeMillis());
        order.setStatus(STATUS_PENDING);
        order.setCreatedAt(LocalDateTime.now());

        double total = 0.0;
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();

        // FIX isu #2: Null-safe guard untuk RequestContextHolder.
        // getRequestAttributes() bisa null jika dipanggil di luar HTTP thread
        // (misal: unit test tanpa mock Spring MVC). Tanpa guard ini akan NPE.
        org.springframework.web.context.request.ServletRequestAttributes requestAttributes =
            (org.springframework.web.context.request.ServletRequestAttributes)
            org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            String token = requestAttributes.getRequest().getHeader("Authorization");
            if (token != null) {
                headers.set("Authorization", token);
            }
        }
        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

        for (OrderRequestDTO.OrderItemRequest req : itemRequests) {
            double realPrice = 0.0;
            try {
                @SuppressWarnings("unchecked")
                org.springframework.http.ResponseEntity<java.util.Map<String, Object>> response = restTemplate.exchange(
                    "http://localhost:8082/api/products/" + req.getProductId(),
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    (Class<java.util.Map<String, Object>>) (Class<?>) java.util.Map.class
                );
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> data = (java.util.Map<String, Object>) response.getBody().get("data");
                    
                    int availableStock = Integer.parseInt(data.get("stock").toString());
                    int requestedQty = req.getQuantity() != null ? req.getQuantity() : 1;
                    
                    if (availableStock < requestedQty) {
                        throw new RuntimeException("Stok tidak mencukupi untuk Produk: " + data.get("name") + ". Sisa stok: " + availableStock);
                    }
                    
                    realPrice = Double.parseDouble(data.get("price").toString());
                } else {
                    throw new RuntimeException("Gagal mengambil data produk ID: " + req.getProductId());
                }
            } catch (RuntimeException re) {
                // Lempar ulang RuntimeException yang spesifik (seperti stok habis)
                throw re;
            } catch (Exception e) {
                throw new RuntimeException("Product Service error atau Produk ID " + req.getProductId() + " tidak ditemukan.");
            }

            OrderItem item = new OrderItem();
            item.setProductId(req.getProductId());
            item.setQuantity(req.getQuantity() != null ? req.getQuantity() : 1);
            item.setPrice(realPrice);
            item.setSubtotal(item.getPrice() * item.getQuantity());
            order.addItem(item);
            total += item.getSubtotal();
        }

        order.setTotalAmount(total);
        Order savedOrder = orderRepository.save(order);

        java.util.List<com.example.saga.event.OrderItemDTO> messageItems = new java.util.ArrayList<>();
        for (OrderItem item : savedOrder.getItems()) {
            messageItems.add(new com.example.saga.event.OrderItemDTO(item.getProductId(), item.getQuantity()));
        }

        com.example.saga.event.OrderCreatedEvent event = new com.example.saga.event.OrderCreatedEvent(
            String.valueOf(savedOrder.getId()),
            savedOrder.getCustomerId(),
            messageItems,
            java.math.BigDecimal.valueOf(savedOrder.getTotalAmount())
        );
        orderProducer.sendOrderCreated(event);
        return savedOrder;
    }

    @Transactional
    public Order confirmPayment(Long orderId, String courierName) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order ID " + orderId + " tidak ditemukan"));

        // EDGE CASE: Jika order sudah dibatalkan secara bersamaan (race condition),
        // uang pelanggan sudah terpotong di Payment Service. Kita harus trigger Refund otomatis.
        if (STATUS_CANCELLED.equals(order.getStatus())) {
            log.warn("[ORDER-SAGA]  ⚠ Edge Case: Payment sukses tapi order sudah CANCELLED | orderId={}. Memicu refund otomatis.", orderId);
            orderProducer.sendRefundPayment(new com.example.saga.event.RefundPaymentEvent(
                String.valueOf(orderId),
                java.math.BigDecimal.valueOf(order.getTotalAmount()),
                "Race condition: Order telah CANCELLED saat pembayaran berhasil."
            ));
            return order; // Tidak throw exception agar proses dianggap selesai (ditangani kompensasi)
        }

        // Terima PENDING (payment manual) atau AWAITING_PAYMENT (Saga otomatis)
        if (!STATUS_PENDING.equals(order.getStatus()) && !STATUS_AWAITING_PAYMENT.equals(order.getStatus())) {
            throw new RuntimeException("Hanya order PENDING atau AWAITING_PAYMENT yang bisa dibayar. Status saat ini: " + order.getStatus());
        }

        order.setStatus(STATUS_PAID);
        Order savedOrder = orderRepository.save(order);

        // Fitur pengiriman otomatis via REST dimatikan agar sesuai Event-Driven Architecture manual.
        // User (atau Admin) akan memanggil endpoint di Shipping Service secara manual,
        // lalu Shipping Service akan mempublish event (misal: OrderShippedEvent) via Kafka.
        log.info("[ORDER-SAGA]    ✓ Menunggu proses pengiriman manual via Shipping Service | orderId={}", orderId);

        return savedOrder;
    }

    @Transactional
    public Order updateStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order ID " + orderId + " tidak ditemukan"));
        order.setStatus(newStatus.toUpperCase());
        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order tidak ditemukan"));

        // Idempotency: Jika sudah CANCELLED, return langsung (hindari duplikasi Kafka event)
        if (STATUS_CANCELLED.equals(order.getStatus())) {
            return order;
        }

        // Tidak bisa cancel jika status PAID — gunakan endpoint /cancel-paid agar refund terjadi
        if (STATUS_PAID.equals(order.getStatus())) {
            throw new RuntimeException(
                "Order sudah PAID. Gunakan endpoint /cancel-paid untuk membatalkan dan mendapat refund.");
        }

        // Tidak bisa cancel jika sudah dikirim atau selesai
        if (STATUS_SHIPPED.equals(order.getStatus()) ||
            STATUS_DELIVERED.equals(order.getStatus()) ||
            STATUS_COMPLETED.equals(order.getStatus())) {
            throw new RuntimeException("Pesanan tidak bisa dibatalkan karena sudah dalam proses pengiriman atau sudah selesai.");
        }

        boolean wasAwaitingPayment = STATUS_AWAITING_PAYMENT.equals(order.getStatus());

        order.setStatus(STATUS_CANCELLED);
        Order savedOrder = orderRepository.save(order);

        // 1. Beritahu Inventory Service untuk mengembalikan stok
        java.util.List<com.example.saga.event.OrderItemDTO> messageItems = new java.util.ArrayList<>();
        for (OrderItem item : savedOrder.getItems()) {
            messageItems.add(new com.example.saga.event.OrderItemDTO(item.getProductId(), item.getQuantity()));
        }

        com.example.saga.event.ReleaseProductReservationEvent releaseEvent = new com.example.saga.event.ReleaseProductReservationEvent(
            String.valueOf(savedOrder.getId()),
            messageItems,
            "Order dibatalkan secara reguler"
        );
        orderProducer.sendOrderCancelled(releaseEvent);

        // 2. Safety net untuk race condition:
        //    Jika status sebelumnya AWAITING_PAYMENT, mungkin payment sudah diproses
        //    oleh Payment Service sebelum cancel ini tiba. Kirim refund event sebagai jaga-jaga.
        //    refundByOrderId() di Payment Service akan ignore ini jika tidak ada payment SUCCESS.
        if (wasAwaitingPayment) {
            com.example.saga.event.RefundPaymentEvent refundEvent = new com.example.saga.event.RefundPaymentEvent(
                String.valueOf(savedOrder.getId()),
                java.math.BigDecimal.valueOf(savedOrder.getTotalAmount()),
                "Safety net refund: order dibatalkan saat AWAITING_PAYMENT"
            );
            orderProducer.sendRefundPayment(refundEvent);
            log.warn("[ORDER-SAGA]    ↩ Safety net refund dikirim       | orderId={} | orderNumber={}", savedOrder.getId(), savedOrder.getOrderNumber());
        }

        // Publish OrderCancelledEvent — untuk audit/notifikasi service
        orderProducer.sendOrderCancelledEvent(
            new com.example.saga.event.OrderCancelledEvent(
                String.valueOf(savedOrder.getId()),
                wasAwaitingPayment ? "Order dibatalkan saat menunggu pembayaran" : "Order dibatalkan oleh sistem/user"
            )
        );

        return savedOrder;
    }

    /**
     * Membatalkan order yang sudah berstatus PAID.
     * Digunakan saat customer/admin ingin membatalkan pesanan setelah pembayaran berhasil
     * namun barang belum dikirim.
     *
     * Alur kompensasi yang dijalankan:
     *   1. Status order diubah ke CANCELLED
     *   2. Inventory Service diberitahu via Kafka → stok dikembalikan
     *   3. Payment Service diberitahu via REST → payment di-refund
     */
    @Transactional
    public Order cancelPaidOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order tidak ditemukan"));

        // Hanya boleh cancel jika status PAID (sudah bayar, belum dikirim)
        if (!STATUS_PAID.equals(order.getStatus())) {
            throw new RuntimeException(
                "Pembatalan dengan refund hanya bisa dilakukan pada order berstatus PAID. "
                + "Status saat ini: " + order.getStatus());
        }

        order.setStatus(STATUS_CANCELLED);
        Order savedOrder = orderRepository.save(order);

        // 1. Kembalikan stok ke Inventory Service via Kafka
        java.util.List<com.example.saga.event.OrderItemDTO> messageItems = new java.util.ArrayList<>();
        for (OrderItem item : savedOrder.getItems()) {
            messageItems.add(new com.example.saga.event.OrderItemDTO(item.getProductId(), item.getQuantity()));
        }
        com.example.saga.event.ReleaseProductReservationEvent releaseEvent = new com.example.saga.event.ReleaseProductReservationEvent(
            String.valueOf(savedOrder.getId()),
            messageItems,
            "Customer membatalkan order yang sudah dibayar"
        );
        orderProducer.sendOrderCancelled(releaseEvent);
        log.info("[ORDER-SAGA]    ✓ Kompensasi stok dikirim ke inventory-service | orderNumber={}", savedOrder.getOrderNumber());

        // 2. Proses refund ke Payment Service via Kafka event
        com.example.saga.event.RefundPaymentEvent refundEvent = new com.example.saga.event.RefundPaymentEvent(
            String.valueOf(savedOrder.getId()),
            java.math.BigDecimal.valueOf(savedOrder.getTotalAmount()),
            "Refund otomatis karena pembatalan pesanan"
        );
        orderProducer.sendRefundPayment(refundEvent);
        log.info("[ORDER-SAGA]    ✓ Refund event dikirim via Kafka              | orderNumber={}", savedOrder.getOrderNumber());

        // Publish OrderCancelledEvent — untuk audit/notifikasi service
        orderProducer.sendOrderCancelledEvent(
            new com.example.saga.event.OrderCancelledEvent(
                String.valueOf(savedOrder.getId()),
                "Customer membatalkan order yang sudah dibayar (refund diproses)"
            )
        );

        return savedOrder;
    }

    @Transactional
    public void updateOrderStatusFromShipping(Long orderId, String shippingStatus) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            if ("DELIVERED".equalsIgnoreCase(shippingStatus)) {
                order.setStatus(STATUS_COMPLETED);
                orderRepository.save(order);
                // Publish OrderCompletedEvent saat barang sudah diterima customer
                orderProducer.sendOrderCompleted(
                    new com.example.saga.event.OrderCompletedEvent(String.valueOf(orderId))
                );
                log.info("[ORDER-SAGA]    ✓ Order DELIVERED → COMPLETED | orderId={}", orderId);
            } else {
                order.setStatus(STATUS_SHIPPED);
                orderRepository.save(order);
            }
        }
    }

    public List<Order> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order tidak ditemukan"));
    }

    public void publishOrderCompletedEvent(Long orderId) {
        orderProducer.sendOrderCompleted(
            new com.example.saga.event.OrderCompletedEvent(String.valueOf(orderId))
        );
        log.info("[ORDER-SAGA]    ✓ SAGA SELESAI: Order berhasil dikirim & sampai | orderId={}", orderId);
    }
}
