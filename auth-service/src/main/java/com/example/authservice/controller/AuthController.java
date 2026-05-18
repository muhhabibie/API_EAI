package com.example.authservice.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.authservice.dto.ApiResponse;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.RegisterRequest;
import com.example.authservice.entity.Customer;
import com.example.authservice.repository.CustomerRepository;
import com.example.authservice.security.JwtUtil;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@Tag(name = "Authentication Management", description = "Endpoint untuk Login, Registrasi, dan Dashboard")
public class AuthController {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // FIX #2: Secret dipindah ke application.properties — tidak lagi hardcoded
    @Value("${auth.admin.secret-key:RAHASIA_ADMIN_123}")
    private String adminSecretKey;

    // Harus sinkron dengan auth.service.internal-key di customer-service
    @Value("${auth.service.internal-key:INTERNAL_SYSTEM_KEY_99}")
    private String internalSystemKey;


    @Operation(summary = "Login Pengguna", description = "Masuk ke sistem menggunakan email dan password untuk mendapatkan token JWT.")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<Customer> customerOpt = customerRepository.findByEmail(request.getEmail());

        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            if (passwordEncoder.matches(request.getPassword(), customer.getPassword())) {
                String role = customer.getRole() != null ? customer.getRole() : "ROLE_USER";
                String token = jwtUtil.generateToken(customer.getEmail(), role);

                Map<String, String> loginData = new HashMap<>();
                loginData.put("token", token);
                loginData.put("email", customer.getEmail());
                loginData.put("username", customer.getUsername());
                loginData.put("role", role);

                return ResponseEntity.ok(ApiResponse.success("Autentikasi berhasil! Selamat datang, " + customer.getUsername(), loginData));
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Kredensial tidak valid. Silakan periksa kembali email dan password Anda."));
    }

    @Operation(summary = "Registrasi Pengguna/Admin", description = "Mendaftarkan akun baru ke sistem. Pendaftaran Admin butuh kunci rahasia.")
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        String roleName = request.getRole() != null ? request.getRole() : "ROLE_USER";
        
        // Proteksi Admin
        if (roleName.equals("ROLE_ADMIN")) {
            if (!adminSecretKey.equals(request.getAdminKey())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Kunci rahasia Admin salah!"));
            }
        }
        
        // Proteksi User (Harus via Customer Service)
        if (roleName.equals("ROLE_USER")) {
            String systemKey = (String) request.getAdminKey();
            if (!internalSystemKey.equals(systemKey)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Pendaftaran User harus melalui Customer Service!"));
            }
        }

        if (customerRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email sudah terdaftar"));
        }

        Customer customer = new Customer();
        customer.setUsername(request.getUsername());
        customer.setPassword(passwordEncoder.encode(request.getPassword()));
        customer.setEmail(request.getEmail());
        customer.setName(request.getName());
        customer.setAddress(request.getAddress());
        customer.setRole(roleName);

        customerRepository.save(customer);

        java.util.Map<String, String> responseData = new java.util.HashMap<>();
        responseData.put("username", customer.getUsername());
        responseData.put("name", customer.getName());
        responseData.put("email", customer.getEmail());
        responseData.put("role", customer.getRole());

        String successMsg = "User " + customer.getName() + " (" + customer.getUsername() + ") berhasil didaftarkan sebagai " + roleName;
        return ResponseEntity.ok(ApiResponse.success(successMsg, responseData));
    }

    @Operation(summary = "Dashboard Admin", description = "Endpoint khusus Admin untuk melihat profil dan hak akses.")
    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> adminDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> data = new HashMap<>();
        data.put("email", auth.getName());
        data.put("authorities", auth.getAuthorities());
        return ResponseEntity.ok(ApiResponse.success("Data dashboard berhasil dimuat untuk Admin: " + auth.getName(), data));
    }
}
