package com.example.customerservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(
    name = "CustomerRequest",
    description = "Body request untuk mendaftarkan customer baru. " +
                  "Endpoint ini sekaligus mensinkronisasi akun ke Auth Service secara otomatis. " +
                  "Gunakan email yang sama untuk login via POST /api/login."
)
public class CustomerRequest {

    @NotBlank(message = "Username tidak boleh kosong")
    @Schema(description = "Username unik untuk identifikasi akun.", example = "muhammad_habibi", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "Nama tidak boleh kosong")
    @Schema(description = "Nama lengkap customer.", example = "Muhammad Habibi", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "Email tidak boleh kosong")
    @Email(message = "Format email salah")
    @Schema(description = "Email unik. Digunakan sebagai kredensial login di Auth Service.", example = "habibi@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Password tidak boleh kosong")
    @Schema(description = "Password akun (minimal 6 karakter).", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "Alamat pengiriman default customer.", example = "Jl. Merdeka No. 10, Jakarta")
    private String address;

    @Schema(description = "Saldo awal customer dalam Rupiah. Default 0 jika tidak diisi.", example = "500000.0")
    private Double balance;

    @Schema(description = "Nomor HP aktif customer.", example = "08123456789")
    private String phone;

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
