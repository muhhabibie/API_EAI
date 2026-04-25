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

        for (ItemRequest req : itemRequests) {
            OrderItem item = new OrderItem();
            item.setProductId(req.getProductId());
            item.setQuantity(req.getQuantity() != null ? req.getQuantity() : 1);
            item.setPrice(req.getPrice() != null ? req.getPrice() : 0.0);
            item.setSubtotal(item.getPrice() * item.getQuantity());
            order.addItem(item);
            total += item.getSubtotal();
        }

        order.setTotalAmount(total);
        Order savedOrder = orderRepository.save(order);

        OrderMessage message = new OrderMessage(
            savedOrder.getId(),
            savedOrder.getOrderNumber(),
            savedOrder.getCustomerId(),
            savedOrder.getStatus(),
            savedOrder.getTotalAmount()
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
        return orderRepository.save(order);
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
