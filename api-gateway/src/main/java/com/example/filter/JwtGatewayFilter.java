package com.example.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;

@Component
public class JwtGatewayFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 1. Lewati request untuk file statis (Frontend) atau selain "/api"
        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Lewati endpoint publik (Login, Register Auth, dll)
        if (path.startsWith("/api/auth") || path.startsWith("/api/login") || path.contains("swagger-ui") || path.contains("api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Register Customer (POST /api/customers) juga publik
        if (path.equals("/api/customers") && request.getMethod().equalsIgnoreCase("POST")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 4. Katalog Produk & Kategori (GET) juga publik
        if ((path.startsWith("/api/products") || path.startsWith("/api/categories")) 
            && request.getMethod().equalsIgnoreCase("GET")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 4. Periksa header Authorization
        final String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"API Gateway: JWT Token is missing or invalid format.\"}");
            response.setContentType("application/json");
            return;
        }

        String jwt = authorizationHeader.substring(7);

        try {
            Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            // Verifikasi tanda tangan dan expired token
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(jwt);
            
            // Jika sukses, teruskan request ke ProxyController
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"API Gateway: Invalid or Expired JWT Token.\"}");
            response.setContentType("application/json");
        }
    }
}
