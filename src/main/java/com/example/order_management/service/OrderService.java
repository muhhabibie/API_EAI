package com.example.order_management.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.order_management.entity.CourierType;
import com.example.order_management.entity.Customer;
import com.example.order_management.entity.Order;
import com.example.order_management.entity.OrderItem;
import com.example.order_management.entity.Product;
import com.example.order_management.repository.CustomerRepository;
import com.example.order_management.repository.OrderRepository;
import com.example.order_management.repository.ProductRepository;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private InventoryService inventoryService; 
    @Autowired
    private ShippingService shippingService;

    @Transactional
    public Order createOrder(Long customerId, List<ItemRequest> itemRequests) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer tidak ditemukan"));

        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderNumber("ORD-" + System.currentTimeMillis());
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());

        double total = 0.0;

        for (ItemRequest req : itemRequests) {
            Product product = productRepository.findById(req.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product tidak ditemukan"));

            // EAI: Otomatis memotong stok dan membuat tiket reservasi ACTIVE
            inventoryService.reserveStock(product.getId(), req.getQuantity());

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(req.getQuantity());
            item.setPrice(product.getPrice());
            item.setSubtotal(product.getPrice() * req.getQuantity());
            order.addItem(item);
            total += item.getSubtotal();
        }

        order.setTotalAmount(total);
        return orderRepository.save(order);
    }

    public List<Order> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order updateStatus(Long orderId, String status) {
        return orderRepository.findById(orderId).map(order -> {
            order.setStatus(status);
            return orderRepository.save(order);
        }).orElse(null);
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order tidak ditemukan dengan id: " + id));
    }

    @Transactional
    public Order cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order tidak ditemukan dengan id: " + id));

        if (order.getStatus().equalsIgnoreCase("SHIPPED") || order.getStatus().equalsIgnoreCase("DELIVERED")) {
            throw new RuntimeException("Order dengan status " + order.getStatus() + " tidak dapat dibatalkan");
        }
        if (order.getStatus().equalsIgnoreCase("CANCELLED")) {
            throw new RuntimeException("Order sudah dibatalkan sebelumnya");
        }

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus("CANCELLED");
        return orderRepository.save(order);
    }

    @Transactional
    public Order confirmPayment(Long orderId, String courierName) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order ID " + orderId + " tidak ditemukan"));

        if (!"PENDING".equalsIgnoreCase(order.getStatus())) {
            throw new RuntimeException("Transaksi gagal: Order sudah berstatus " + order.getStatus());
        }

        // 1. Update Status Order
        order.setStatus("PAID");
        Order savedOrder = orderRepository.save(order);

        // 2. Konfirmasi Inventory (Ubah dari ACTIVE jadi COMPLETED)
        for (OrderItem item : order.getItems()) {
            inventoryService.confirmReservation(item.getProduct().getId(), item.getQuantity());
        }

        // 3. Integrasi ke Shipping (Generate Resi otomatis)
        CourierType courierEnum = CourierType.valueOf(courierName.toUpperCase());
        shippingService.createShipment(order.getId(), courierEnum);

        return savedOrder;
    }

    // FITUR BARU: Update status Order jika kurir update status pengiriman
    @Transactional
    public void updateOrderStatusFromShipping(Long orderId, String shippingStatus) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            if ("IN_TRANSIT".equalsIgnoreCase(shippingStatus)) {
                order.setStatus("SHIPPED");
            } else if ("DELIVERED".equalsIgnoreCase(shippingStatus)) {
                order.setStatus("COMPLETED");
            }
            orderRepository.save(order);
        }
    }

    public static class ItemRequest {
        private Long productId;
        private Integer quantity;
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}