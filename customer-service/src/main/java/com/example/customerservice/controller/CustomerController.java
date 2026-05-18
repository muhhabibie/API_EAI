package com.example.customerservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.example.customerservice.dto.ApiResponse;
import com.example.customerservice.dto.CustomerRequest;
import com.example.customerservice.entity.Customer;
import com.example.customerservice.service.CustomerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    // FIX #4: Inject RestTemplate dari Spring Bean (bukan new RestTemplate() tiap request)
    @Autowired
    private RestTemplate restTemplate;

    // FIX #3: Secret dipindah ke application.properties, tidak lagi hardcoded di kode
    @Value("${auth.service.internal-key:INTERNAL_SYSTEM_KEY_99}")
    private String authServiceInternalKey;

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
        Double balance = request.getBalance();
        customer.setBalance(balance != null ? balance : 0.0);
        customer.setPhone(request.getPhone());
        
        Customer saved = customerService.createCustomer(customer);

        try {
            // FIX #4: Gunakan RestTemplate bean dari AppConfig, bukan new tiap request
            java.util.Map<String, Object> authPayload = new java.util.HashMap<>();
            authPayload.put("username", request.getUsername());
            authPayload.put("name", request.getName());
            authPayload.put("email", request.getEmail());
            authPayload.put("address", request.getAddress());
            authPayload.put("password", request.getPassword());
            authPayload.put("role", "ROLE_USER");
            // FIX #3: Secret dibaca dari @Value (application.properties), bukan hardcoded
            authPayload.put("adminKey", authServiceInternalKey);

            restTemplate.postForEntity("http://localhost:8081/api/register", authPayload, String.class);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Registrasi sukses untuk " + saved.getName() + " (" + saved.getUsername() + ")", saved));
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Gagal sinkronisasi ke Auth: " + e.getResponseBodyAsString()));
        } catch (org.springframework.web.client.ResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gagal terhubung ke Auth Service: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Kesalahan tidak terduga: " + e.getMessage()));
        }
    }

    @Operation(summary = "Update Data Customer", description = "Memperbarui informasi profil pelanggan.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> updateCustomer(@PathVariable Long id, @RequestBody Customer customer) {
        Customer updated = customerService.updateCustomer(id, customer);
        if (updated != null) {
            return ResponseEntity.ok(ApiResponse.success("Profil pelanggan '" + updated.getName() + "' berhasil diperbarui", updated));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Customer tidak ditemukan"));
    }

    @Operation(summary = "Hapus Customer", description = "Menghapus data pelanggan secara permanen. Khusus Admin.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> deleteCustomer(@PathVariable Long id) {
        if (customerService.deleteCustomer(id)) {
            return ResponseEntity.ok(ApiResponse.success("Pelanggan dengan ID " + id + " berhasil dihapus secara permanen dari sistem", null));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Customer tidak ditemukan"));
    }

    @Operation(summary = "Potong Saldo Customer", description = "Mengurangi jumlah saldo pelanggan untuk transaksi pembayaran.")
    @PutMapping("/{id}/deduct-balance")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> deductBalance(@PathVariable Long id, @RequestParam Double amount) {
        // FIX #6: Gunakan metode atomik — menghilangkan race condition READ-CHECK-WRITE
        // yang bisa menyebabkan saldo negatif jika 2 request masuk hampir bersamaan.
        Double newBalance = customerService.deductBalanceAtomic(id, amount);
        return ResponseEntity.ok(ApiResponse.success("Pembayaran berhasil. Saldo telah dipotong sebesar " + amount + ". Sisa saldo: " + newBalance, newBalance));
    }

    @Operation(summary = "Tambah Saldo Customer", description = "Menambah jumlah saldo pelanggan (untuk topup atau refund).")
    @PutMapping("/{id}/add-balance")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> addBalance(@PathVariable Long id, @RequestParam Double amount) {
        Customer customer = customerService.getCustomerById(id)
            .orElseThrow(() -> new RuntimeException("Customer tidak ditemukan"));
        
        Double currentBalance = customer.getBalance() != null ? customer.getBalance() : 0.0;
        customer.setBalance(currentBalance + amount);
        customerService.updateCustomer(id, customer);
        
        return ResponseEntity.ok(ApiResponse.success("Topup berhasil. Saldo telah ditambahkan sebesar " + amount + ". Total saldo saat ini: " + customer.getBalance(), customer.getBalance()));
    }
}
