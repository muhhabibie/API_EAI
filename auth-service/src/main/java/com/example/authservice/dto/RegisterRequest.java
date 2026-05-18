package com.example.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    name = "RegisterRequest",
    description = "Body request untuk registrasi akun baru. Untuk mendaftar sebagai ROLE_USER, " +
                  "gunakan endpoint POST /api/customers (Customer Service) bukan endpoint ini langsung. " +
                  "Endpoint ini dipakai Customer Service secara internal."
)
public class RegisterRequest {

    @Schema(description = "Username unik untuk identifikasi akun.", example = "muhammad_habibi", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "Password akun (akan di-hash sebelum disimpan).", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "Alamat email unik. Digunakan untuk login.", example = "habibi@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "Nama lengkap pengguna.", example = "Muhammad Habibi")
    private String name;

    @Schema(description = "Alamat lengkap pengguna.", example = "Jl. Merdeka No. 10, Jakarta")
    private String address;

    @Schema(
        description = "Role akun: ROLE_USER atau ROLE_ADMIN. Default: ROLE_USER.",
        example = "ROLE_USER",
        allowableValues = {"ROLE_USER", "ROLE_ADMIN"}
    )
    private String role;

    @Schema(
        description = "Kunci rahasia. Wajib diisi jika role = ROLE_ADMIN. Untuk ROLE_USER diisi otomatis oleh Customer Service.",
        example = "RAHASIA_ADMIN_123"
    )
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
