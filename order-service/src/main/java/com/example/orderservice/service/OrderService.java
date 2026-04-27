package com.example.orderservice.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.kafka.OrderProducer;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.model.OrderMessage;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderProducer orderProducer;

    @Autowired
    private com.example.orderservice.security.JwtUtil jwtUtil;

    // ========== STATUS CONSTANTS ==========
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_SHIPPED = "SHIPPED";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    // ========== CREATE ORDER ==========
    @Transactional
    public Order createOrder(Long customerId, List<ItemRequest> itemRequests) {
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

        // Siapkan RestTemplate untuk memanggil Product Service
        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        
        // Ambil Token JWT dari request saat ini untuk diteruskan ke Product Service
        jakarta.servlet.http.HttpServletRequest currentRequest = 
            ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()).getRequest();
        String token = currentRequest.getHeader("Authorization");
        if (token != null) {
            headers.set("Authorization", token);
        }
        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

        for (ItemRequest req : itemRequests) {
            // Ambil harga asli dari Product Service
            double realPrice = 0.0;
            try {
                org.springframework.http.ResponseEntity<java.util.Map> response = restTemplate.exchange(
                    "http://localhost:8082/api/products/" + req.getProductId(),
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    java.util.Map.class
                );
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    java.util.Map<String, Object> data = (java.util.Map<String, Object>) response.getBody().get("data");
                    realPrice = Double.parseDouble(data.get("price").toString());
                } else {
                    throw new RuntimeException("Gagal mengambil data produk ID: " + req.getProductId());
                }
            } catch (Exception e) {
                throw new RuntimeException("Product Service error atau Produk ID " + req.getProductId() + " tidak ditemukan. " + e.getMessage());
            }

            OrderItem item = new OrderItem();
            item.setProductId(req.getProductId());
            item.setQuantity(req.getQuantity() != null ? req.getQuantity() : 1);
            item.setPrice(realPrice); // Gunakan harga asli! Jangan gunakan req.getPrice()
            item.setSubtotal(item.getPrice() * item.getQuantity());
            order.addItem(item);
            total += item.getSubtotal();
        }

        order.setTotalAmount(total);
        Order savedOrder = orderRepository.save(order);

        java.util.List<OrderMessage.OrderItemMessage> messageItems = new java.util.ArrayList<>();
        for (OrderItem item : savedOrder.getItems()) {
            messageItems.add(new OrderMessage.OrderItemMessage(item.getProductId(), item.getQuantity()));
        }

        OrderMessage message = new OrderMessage(
            savedOrder.getId(),
            savedOrder.getOrderNumber(),
            savedOrder.getCustomerId(),
            savedOrder.getStatus(),
            savedOrder.getTotalAmount(),
            messageItems
        );
        orderProducer.sendOrderCreated(message);
        return savedOrder;
    }

    // ========== CONFIRM PAYMENT (User bayar) ==========
    // PENDING → PAID
    @Transactional
    public Order confirmPayment(Long orderId, String courierName) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order ID " + orderId + " tidak ditemukan"));

        if (!STATUS_PENDING.equals(order.getStatus())) {
            throw new RuntimeException("Pembayaran gagal: Order berstatus " + order.getStatus()
                    + ". Hanya order PENDING yang bisa dibayar");
        }

        order.setStatus(STATUS_PAID);
        Order savedOrder = orderRepository.save(order);

        // --- MENGHUBUNGI SHIPPING SERVICE ---
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtUtil.generateSystemToken());
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("orderId", orderId);
            payload.put("courierName", courierName);

            org.springframework.http.HttpEntity<java.util.Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(payload, headers);

            restTemplate.postForEntity(
                "http://localhost:8086/api/shipments",
                entity,
                String.class
            );
        } catch (Exception e) {
            System.err.println("PERINGATAN: Gagal membuat Shipment di Shipping Service untuk Order ID " + orderId + ". Error: " + e.getMessage());
            // Dalam sistem riil, ini harus masuk DLQ atau di-retry otomatis.
        }

        return savedOrder;
    }

    // ========== UPDATE STATUS (Admin) ==========
    // Validasi transisi: PAID → PROCESSING → SHIPPED → DELIVERED → COMPLETED
    @Transactional
    public Order updateStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order ID " + orderId + " tidak ditemukan"));

        String currentStatus = order.getStatus();
        newStatus = newStatus.toUpperCase();

        // Validasi transisi status
        boolean validTransition = switch (newStatus) {
            case STATUS_PROCESSING -> STATUS_PAID.equals(currentStatus);
            case STATUS_SHIPPED -> STATUS_PROCESSING.equals(currentStatus);
            case STATUS_DELIVERED -> STATUS_SHIPPED.equals(currentStatus);
            case STATUS_COMPLETED -> STATUS_DELIVERED.equals(currentStatus);
            default -> false;
        };

        if (!validTransition) {
            throw new RuntimeException("Transisi status tidak valid: " + currentStatus + " → " + newStatus
                    + ". Alur yang benar: PENDING → PAID → PROCESSING → SHIPPED → DELIVERED → COMPLETED");
        }

        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    // ========== CANCEL ORDER ==========
    // Hanya bisa cancel saat PENDING atau PAID
    @Transactional
    public Order cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order tidak ditemukan dengan id: " + id));

        if (!STATUS_PENDING.equals(order.getStatus()) && !STATUS_PAID.equals(order.getStatus())) {
            throw new RuntimeException("Order tidak bisa dibatalkan. Status saat ini: " + order.getStatus()
                    + ". Hanya order PENDING atau PAID yang bisa dibatalkan");
        }

        order.setStatus(STATUS_CANCELLED);
        return orderRepository.save(order);
    }

    // ========== UPDATE FROM SHIPPING ==========
    // Dipanggil saat shipment status berubah
    @Transactional
    public void updateOrderStatusFromShipping(Long orderId, String shippingStatus) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            if ("PICKED_UP".equalsIgnoreCase(shippingStatus) || "IN_TRANSIT".equalsIgnoreCase(shippingStatus)) {
                order.setStatus(STATUS_SHIPPED);
            } else if ("DELIVERED".equalsIgnoreCase(shippingStatus)) {
                order.setStatus(STATUS_COMPLETED);
            }
            orderRepository.save(order);
        }
    }

    // ========== QUERIES ==========
    public List<Order> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order tidak ditemukan dengan id: " + id));
    }

    // ========== DTO ==========
    public static class ItemRequest {
        private Long productId;
        private Integer quantity;
        private Double price;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
    }
}
