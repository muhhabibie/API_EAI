package com.example.order_management.controller;

import com.example.order_management.entity.Customer;
import com.example.order_management.repository.CustomerRepository;
import com.example.order_management.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Mengizinkan akses dari frontend
public class AuthController {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // Class bantuan untuk menerima data dari frontend
    public static class LoginRequest {
        public String email;
        public String password;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        // 1. Cari customer berdasarkan email
        Optional<Customer> customerOpt = customerRepository.findByEmail(loginRequest.email);

        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            
            // 2. Cek apakah password cocok
            // Catatan: Jika saat ini password di database belum di-enkripsi, ganti baris ini sementara menjadi:
            // if (loginRequest.password.equals(customer.getPassword())) {
            if (passwordEncoder.matches(loginRequest.password, customer.getPassword())) {
                
                // 3. Jika cocok, buatkan JWT Token!
                String token = jwtUtil.generateToken(customer.getEmail());
                
                // 4. Kirim token ke frontend
                Map<String, String> response = new HashMap<>();
                response.put("token", token);
                response.put("message", "Login Berhasil!");
                
                return ResponseEntity.ok(response);
            }
        }
        
        // Jika email tidak ada atau password salah
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email atau password salah");
    }
}