package com.example.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger Aggregator di API Gateway.
 *
 * Cara kerja:
 * - Swagger UI di Gateway (port 8080) memuat daftar API dari masing-masing service
 *   melalui endpoint /v3/api-docs mereka (langsung dari browser ke port service).
 * - Semua REQUEST pengujian dikirim ke Gateway (port 8080) via ProxyController.
 * - Jadi satu Swagger UI bisa test semua service sekaligus melalui satu pintu (port 8080).
 *
 * Akses: http://localhost:8080/swagger-ui.html
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "API Gateway — EAI Microservices",
        version = "v1",
        description = "Semua endpoint dari seluruh microservice dalam satu halaman Swagger. " +
                      "Gunakan dropdown di pojok kanan atas untuk berpindah antar service. " +
                      "Klik Authorize dan masukkan JWT token sebelum melakukan request."
    ),
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer",
    description = "Masukkan JWT token dari endpoint POST /api/auth/login"
)
public class SwaggerConfig {
}
