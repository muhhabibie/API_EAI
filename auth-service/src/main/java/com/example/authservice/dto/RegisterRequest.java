package com.example.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class RegisterRequest {
    @Schema(example = "muhammad_habibi")
    private String username;
    
    @Schema(example = "password123")
    private String password;
    
    @Schema(example = "user@example.com")
    private String email;
    
    @Schema(example = "Muhammad Habibi")
    private String name;
    
    @Schema(example = "Jl. Merdeka No. 10, Jakarta")
    private String address;
    
    @Schema(example = "ROLE_USER", description = "ROLE_USER atau ROLE_ADMIN")
    private String role;
    
    @Schema(example = "RAHASIA_ADMIN_123", description = "Hanya diisi jika ingin mendaftar sebagai ADMIN")
    private String adminKey;

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getAdminKey() { return adminKey; }
    public void setAdminKey(String adminKey) { this.adminKey = adminKey; }
}
