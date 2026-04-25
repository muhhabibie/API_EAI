package com.example.authservice.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.authservice.dto.ApiResponse;
import com.example.authservice.entity.Customer;
import com.example.authservice.repository.CustomerRepository;
import com.example.authservice.security.JwtUtil;
import com.example.authservice.entity.Customer;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public static class LoginRequest {
        public String email;
        public String password;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Optional<Customer> customerOpt = customerRepository.findByEmail(loginRequest.email);

        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();

            if (passwordEncoder.matches(loginRequest.password, customer.getPassword())) {
                String role = "admin".equalsIgnoreCase(customer.getUsername()) ? "ROLE_ADMIN" : "ROLE_USER";
                String token = jwtUtil.generateToken(customer.getEmail(), role);

                Map<String, String> loginData = new HashMap<>();
                loginData.put("token", token);
                loginData.put("email", customer.getEmail());
                loginData.put("name", customer.getName());
                loginData.put("role", role);

                return ResponseEntity.ok(ApiResponse.success("Login Berhasil!", loginData));
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Email atau password salah"));
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> adminDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> data = new HashMap<>();
        data.put("email", auth.getName());
        data.put("authorities", auth.getAuthorities());
        return ResponseEntity.ok(ApiResponse.success("Selamat datang Admin!", data));
    }

    @GetMapping("/user/profile")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> userProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> data = new HashMap<>();
        data.put("email", auth.getName());
        data.put("authorities", auth.getAuthorities());
        return ResponseEntity.ok(ApiResponse.success("Profil User", data));
    }

    @GetMapping("/public/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(ApiResponse.success("Auth Service is Running", null));
    }

    // ========== FITUR REGISTER (TAMBAHAN) ==========
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Customer customer) {
        // Cek apakah email sudah terdaftar
        if (customerRepository.findByEmail(customer.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(com.example.authservice.dto.ApiResponse.error("Email sudah terdaftar!"));
        }

        // Encode password biar aman di DB
        customer.setPassword(passwordEncoder.encode(customer.getPassword()));

        // Simpan ke database
        Customer savedCustomer = customerRepository.save(customer);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(com.example.authservice.dto.ApiResponse.success("Registrasi Berhasil!", savedCustomer));
    }
}
