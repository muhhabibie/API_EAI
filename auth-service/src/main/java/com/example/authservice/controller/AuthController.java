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
                String role = "admin".equalsIgnoreCase(customer.getUsername())
                        ? "ROLE_ADMIN"
                        : "ROLE_USER";

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

        @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String email = request.get("email");
        String username = request.get("username");
        String password = request.get("password");
        String address = request.getOrDefault("address", "");

        if (email == null || password == null || name == null || username == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Field name, email, username, dan password wajib diisi"));
        }

        if (customerRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Email sudah terdaftar"));
        }

        Customer newCustomer = new Customer();
        newCustomer.setName(name);
        newCustomer.setEmail(email);
        newCustomer.setUsername(username);
        newCustomer.setPassword(passwordEncoder.encode(password)); // Hash otomatis
        newCustomer.setAddress(address);

        customerRepository.save(newCustomer);

        Map<String, String> data = new HashMap<>();
        data.put("email", newCustomer.getEmail());
        data.put("username", newCustomer.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registrasi berhasil", data));
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
}
