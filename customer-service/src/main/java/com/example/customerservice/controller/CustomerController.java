package com.example.customerservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.customerservice.dto.ApiResponse;
import com.example.customerservice.dto.CustomerRequest;
import com.example.customerservice.entity.Customer;
import com.example.customerservice.service.CustomerService;

import jakarta.validation.Valid;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/customers")
@Tag(name = "Customer Management", description = "Endpoint untuk mengelola data dan saldo pelanggan")
public class CustomerController {
    
    @Autowired
    private CustomerService customerService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Operation(summary = "Ambil Semua Customer", description = "Melihat daftar seluruh pelanggan. Khusus Admin.")
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getAllCustomers() {
        List<Customer> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(ApiResponse.success(customers));
    }

    @Operation(summary = "Ambil Customer by ID", description = "Melihat informasi detail profil pelanggan tertentu.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> getCustomerById(@PathVariable Long id) {
        return customerService.getCustomerById(id)
                .map(customer -> ResponseEntity.ok(ApiResponse.success(customer)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Customer tidak ditemukan")));
    }

    @Operation(summary = "Daftar Customer Baru", description = "Mendaftarkan profil pelanggan baru dan otomatis tersinkronisasi ke Auth Service.")
    @PostMapping
    public ResponseEntity<?> createCustomer(@RequestBody @Valid CustomerRequest request) {
        Customer customer = new Customer();
        customer.setUsername(request.getUsername());
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setAddress(request.getAddress());
        customer.setPassword(passwordEncoder.encode(request.getPassword()));
        customer.setBalance(request.getBalance() != null ? request.getBalance() : 0.0);
        customer.setPhone(request.getPhone());
        
        Customer saved = customerService.createCustomer(customer);

        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            java.util.Map<String, Object> authPayload = new java.util.HashMap<>();
            authPayload.put("username", request.getUsername()); 
            authPayload.put("name", request.getName());
            authPayload.put("email", request.getEmail());
            authPayload.put("address", request.getAddress());
            authPayload.put("password", request.getPassword());
            authPayload.put("role", "ROLE_USER"); 
            authPayload.put("adminKey", "INTERNAL_SYSTEM_KEY_99"); // Kunci rahasia untuk sistem
            
            restTemplate.postForEntity("http://localhost:8081/api/register", authPayload, String.class);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Registrasi sukses!", saved));
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Gagal sinkronisasi ke Auth: " + e.getResponseBodyAsString()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gagal terhubung ke Auth Service: " + e.getMessage()));
        }
    }

    @Operation(summary = "Update Data Customer", description = "Memperbarui informasi profil pelanggan.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> updateCustomer(@PathVariable Long id, @RequestBody Customer customer) {
        Customer updated = customerService.updateCustomer(id, customer);
        if (updated != null) {
            return ResponseEntity.ok(ApiResponse.success("Customer berhasil diupdate", updated));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Customer tidak ditemukan"));
    }

    @Operation(summary = "Hapus Customer", description = "Menghapus data pelanggan secara permanen. Khusus Admin.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> deleteCustomer(@PathVariable Long id) {
        if (customerService.deleteCustomer(id)) {
            return ResponseEntity.ok(ApiResponse.success("Customer berhasil dihapus", null));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Customer tidak ditemukan"));
    }

    @Operation(summary = "Potong Saldo Customer", description = "Mengurangi jumlah saldo pelanggan untuk transaksi pembayaran.")
    @PutMapping("/{id}/deduct-balance")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> deductBalance(@PathVariable Long id, @RequestParam Double amount) {
        Customer customer = customerService.getCustomerById(id)
            .orElseThrow(() -> new RuntimeException("Customer tidak ditemukan"));
        
        if (customer.getBalance() == null || customer.getBalance() < amount) {
            throw new RuntimeException("Saldo tidak cukup untuk transaksi ini");
        }
        
        customer.setBalance(customer.getBalance() - amount);
        customerService.updateCustomer(id, customer);
        
        return ResponseEntity.ok(ApiResponse.success("Saldo berhasil dipotong", customer.getBalance()));
    }
}
