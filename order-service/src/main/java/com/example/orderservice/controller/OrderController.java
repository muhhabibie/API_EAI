package com.example.orderservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.orderservice.dto.ApiResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.service.OrderService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;

    // ADMIN + USER bisa buat order
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> createOrder(@RequestParam Long customerId,
            @RequestBody List<OrderService.ItemRequest> items) {
        Order created = orderService.createOrder(customerId, items);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order berhasil dibuat", created));
    }

    // ADMIN bisa lihat semua order, USER juga bisa (filter by customerId)
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> getAllOrders(@RequestParam(required = false) Long customerId) {
        List<Order> orders;
        if (customerId != null) {
            orders = orderService.getOrdersByCustomer(customerId);
        } else {
            orders = orderService.getAllOrders();
        }
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    // Hanya ADMIN bisa update status order
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam String status) {
        Order updated = orderService.updateStatus(id, status);
        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Order tidak ditemukan"));
        }
        return ResponseEntity.ok(ApiResponse.success("Status order berhasil diupdate", updated));
    }

    // ADMIN + USER bisa lihat detail order
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    // ADMIN + USER bisa cancel order
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
        Order cancelledOrder = orderService.cancelOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Order berhasil dibatalkan", cancelledOrder));
    }

    // ADMIN + USER bisa bayar order
    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> payOrder(
        @PathVariable Long id, 
        @RequestParam ("courier") String courierName) {
        Order paidOrder = orderService.confirmPayment(id, courierName);
        return ResponseEntity.ok(ApiResponse.success("Pembayaran berhasil dikonfirmasi", paidOrder));
    }
}
