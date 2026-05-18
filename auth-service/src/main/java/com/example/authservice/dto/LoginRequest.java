package com.example.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    name = "LoginRequest",
    description = "Body request untuk login. Gunakan email dan password yang sudah terdaftar."
)
public class LoginRequest {

    @Schema(
        description = "Email terdaftar. Untuk login sebagai Admin gunakan email admin.",
        example = "admin@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String email;

    @Schema(
        description = "Password akun.",
        example = "admin123",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String password;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
