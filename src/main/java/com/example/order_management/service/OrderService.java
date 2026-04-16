package com.example.order_management.service;

import com.example.order_management.entity.*;
import com.example.order_management.repository.*;
import com.example.order_management.entity.Product;
import com.example.order_management.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ProductRepository productRepository;

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

            if (product.getStock() < req.getQuantity()) {
                throw new RuntimeException("Stok produk tidak cukup: " + product.getName());
            }

            product.setStock(product.getStock() - req.getQuantity());
            productRepository.save(product);

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

    public static class ItemRequest {
        private Long productId;
        private Integer quantity;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
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

        // Kembalikan stok produk
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus("CANCELLED");
        return orderRepository.save(order);
    }
}