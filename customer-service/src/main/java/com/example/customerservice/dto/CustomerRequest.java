package com.example.customerservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class CustomerRequest {
    
    @NotBlank(message = "Username tidak boleh kosong")
    @Schema(example = "muhammad_habibi")
    private String username;

    @NotBlank(message = "Nama tidak boleh kosong")
    @Schema(example = "Muhammad Habibi")
    private String name;

    @NotBlank(message = "Email tidak boleh kosong")
    @Email(message = "Format email salah")
    @Schema(example = "user@example.com")
    private String email;

    @NotBlank(message = "Password tidak boleh kosong")
    @Schema(example = "password123")
    private String password;

    @Schema(example = "Jl. Merdeka No. 10, Jakarta")
    private String address;

    @Schema(example = "500000.0", description = "Saldo awal customer")
    private Double balance;

    @Schema(example = "08123456789", description = "Nomor HP customer")
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
