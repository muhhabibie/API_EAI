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
import com.example.orderservice.dto.OrderRequestDTO;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderProducer orderProducer;

    @Autowired
    private com.example.orderservice.security.JwtUtil jwtUtil;

    public static final String STATUS_PENDING = "PENDING";
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
        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        
        jakarta.servlet.http.HttpServletRequest currentRequest = 
            ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()).getRequest();
        String token = currentRequest.getHeader("Authorization");
        if (token != null) {
            headers.set("Authorization", token);
        }
        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

        for (OrderRequestDTO.OrderItemRequest req : itemRequests) {
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

    @Transactional
    public Order confirmPayment(Long orderId, String courierName) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order ID " + orderId + " tidak ditemukan"));

        if (!STATUS_PENDING.equals(order.getStatus())) {
            throw new RuntimeException("Hanya order PENDING yang bisa dibayar");
        }

        order.setStatus(STATUS_PAID);
        Order savedOrder = orderRepository.save(order);

        String receiverName = "Customer-" + order.getCustomerId();
        String deliveryAddress = "Alamat tidak tersedia";
        Double shippingFee = 15000.0;

        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.HttpHeaders authHeaders = new org.springframework.http.HttpHeaders();
            authHeaders.set("Authorization", "Bearer " + jwtUtil.generateSystemToken());
            org.springframework.http.HttpEntity<String> authEntity = new org.springframework.http.HttpEntity<>(authHeaders);

            org.springframework.http.ResponseEntity<java.util.Map> customerResponse = restTemplate.exchange(
                "http://localhost:8083/api/customers/" + order.getCustomerId(),
                org.springframework.http.HttpMethod.GET,
                authEntity,
                java.util.Map.class
            );

            if (customerResponse.getStatusCode().is2xxSuccessful() && customerResponse.getBody() != null) {
                java.util.Map<String, Object> body = (java.util.Map<String, Object>) customerResponse.getBody().get("data");
                if (body != null) {
                    receiverName = body.get("name").toString();
                    deliveryAddress = body.get("address").toString();
                }
            }
        } catch (Exception e) {}

        if (courierName.toUpperCase().contains("YES") || courierName.toUpperCase().contains("EXPRESS")) {
            shippingFee = 25000.0;
        }

        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtUtil.generateSystemToken());
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("orderId", orderId);
            payload.put("courierName", courierName);
            payload.put("receiverName", receiverName);
            payload.put("deliveryAddress", deliveryAddress);
            payload.put("shippingFee", shippingFee);

            org.springframework.http.HttpEntity<java.util.Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(payload, headers);
            restTemplate.postForEntity("http://localhost:8086/api/shipments", entity, String.class);
        } catch (Exception e) {}

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

        // Aturan: Tidak bisa cancel jika sudah dikirim atau selesai
        if (STATUS_SHIPPED.equals(order.getStatus()) || 
            STATUS_DELIVERED.equals(order.getStatus()) || 
            STATUS_COMPLETED.equals(order.getStatus())) {
            throw new RuntimeException("Pesanan tidak bisa dibatalkan karena sudah dalam proses pengiriman atau sudah selesai.");
        }

        order.setStatus(STATUS_CANCELLED);
        Order savedOrder = orderRepository.save(order);

        // Beritahu Inventory Service lewat Kafka untuk mengembalikan stok
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
        orderProducer.sendOrderCancelled(message);

        return savedOrder;
    }

    @Transactional
    public void updateOrderStatusFromShipping(Long orderId, String shippingStatus) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            if ("DELIVERED".equalsIgnoreCase(shippingStatus)) {
                order.setStatus(STATUS_COMPLETED);
            } else {
                order.setStatus(STATUS_SHIPPED);
            }
            orderRepository.save(order);
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
}
