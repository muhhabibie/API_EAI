package com.example.order_management.controller;

import com.example.order_management.entity.Order;
import com.example.order_management.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestParam Long customerId,
            @RequestBody List<OrderService.ItemRequest> items) {
        Order created = orderService.createOrder(customerId, items);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders(@RequestParam(required = false) Long customerId) {
        // 1. Cek apakah ada parameter ?customerId= di URL
        if (customerId != null) {
            // 2. Jika ada, panggil fungsi filter yang baru saja kamu buat
            return ResponseEntity.ok(orderService.getOrdersByCustomer(customerId));
        }
        // 3. Jika tidak ada parameter, baru tampilkan semuanya
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Order> updateStatus(@PathVariable Long id, @RequestParam String status) {
        Order updated = orderService.updateStatus(id, status);
        return updated == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Order> cancelOrder(@PathVariable Long id) {
        Order cancelledOrder = orderService.cancelOrder(id);
        return ResponseEntity.ok(cancelledOrder);
    }

}