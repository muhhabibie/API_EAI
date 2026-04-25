package com.example.customerservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.customerservice.dto.ApiResponse;
import com.example.customerservice.dto.CustomerRequest;
import com.example.customerservice.entity.Customer;
import com.example.customerservice.service.CustomerService;

import jakarta.validation.Valid;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/customers")
public class CustomerController {
    
    @Autowired
    private CustomerService customerService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Hanya ADMIN bisa lihat semua customer
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getAllCustomers() {
        List<Customer> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(ApiResponse.success(customers));
    }

    // ADMIN + USER bisa lihat detail customer
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> getCustomerById(@PathVariable Long id) {
        return customerService.getCustomerById(id)
                .map(customer -> ResponseEntity.ok(ApiResponse.success(customer)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Customer tidak ditemukan")));
    }

    // Registrasi — terbuka tanpa token (diatur di SecurityConfig)
    @PostMapping
    public ResponseEntity<?> createCustomer(@RequestBody @Valid CustomerRequest request) {
        // 1. Simpan ke customer_db (tanpa password, atau password diabaikan)
        Customer customer = new Customer();
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setAddress(request.getAddress());
        // Jika entity Customer masih butuh field password untuk non-null, kita set dummy
        customer.setPassword("SECRET_DI_AUTH_SERVICE"); 
        
        Customer saved = customerService.createCustomer(customer);

        // 2. Panggil Auth Service untuk sinkronisasi kredensial (Opsi 2)
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            
            java.util.Map<String, String> authPayload = new java.util.HashMap<>();
            // Bisa disesuaikan logic jika ingin daftar sbg admin
            authPayload.put("username", "user"); 
            authPayload.put("name", request.getName());
            authPayload.put("email", request.getEmail());
            authPayload.put("address", request.getAddress());
            // Kirim password asli agar di-hash oleh Auth Service
            authPayload.put("password", request.getPassword()); 
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<java.util.Map<String, String>> entity = new org.springframework.http.HttpEntity<>(authPayload, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity("http://localhost:8081/api/register", entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.CREATED) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("Customer profil dibuat dan akun berhasil sinkron ke Auth Service", saved));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("Profil dibuat, tapi gagal registrasi ke Auth Service"));
            }
        } catch (Exception e) {
            System.err.println("Gagal panggil Auth Service: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Customer berhasil disimpan, tapi koneksi ke Auth Service gagal: " + e.getMessage()));
        }
    }

    // ADMIN + USER bisa update customer
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

    // Hanya ADMIN bisa hapus customer
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> deleteCustomer(@PathVariable Long id) {
        if (customerService.deleteCustomer(id)) {
            return ResponseEntity.ok(ApiResponse.success("Customer berhasil dihapus", null));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Customer tidak ditemukan"));
    }
}
