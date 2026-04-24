package com.example.order_management.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class CustomerRequest {
    @NotBlank(message = "Nama customer wajib diisi")
    private String name;

    @Email(message = "Format email tidak valid")
    @NotBlank(message = "Email wajib diisi")
    private String email;

    @NotBlank(message = "Alamat wajib diisi")
    private String address;

    // TAMBAHAN: Menerima password dari frontend
    @NotBlank(message = "Password wajib diisi")
    private String password;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    // TAMBAHAN: Getter & Setter untuk password
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}