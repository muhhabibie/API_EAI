package com.example.order_management.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Panggil filter yang baru saja kita buat
    @Autowired
    private JwtRequestFilter jwtRequestFilter;

@Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) 
            .authorizeHttpRequests(auth -> auth
                // 1. DAFTAR JALUR YANG DIBEBASKAN (TIDAK BUTUH TOKEN)
                .requestMatchers(
                        "/api/login",       // Untuk proses login
                        "/api/customers",   // Untuk proses daftar akun (Register)
                        "/api/products",    // Untuk melihat daftar produk di halaman utama
                        "/user/**",         // Untuk mengakses file HTML/JS/CSS Frontend User
                        "/admin/**",        // Untuk mengakses file HTML/JS/CSS Frontend Admin
                        "/css/**"   ,        // Untuk mengakses file styling
                        "/favicon.ico"
                ).permitAll() 
                
                // 2. SISANYA WAJIB PAKAI TOKEN (seperti Checkout, Riwayat Order, dll)
                .anyRequest().authenticated() 
            )
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}