package com.example.order_management.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // 1. Ambil header "Authorization" dari request frontend
        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        // 2. Cek apakah headernya ada dan dimulai dengan kata "Bearer "
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7); // Potong teks "Bearer " untuk mengambil token aslinya
            try {
                username = jwtUtil.extractUsername(jwt); // Ambil email/username dari dalam token
            } catch (Exception e) {
                System.out.println("Token tidak valid atau sudah kedaluwarsa");
            }
        }

        // 3. Jika token memiliki username dan user belum terautentikasi di sistem
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            // 4. Pastikan token ini benar-benar valid (belum expired dan milik user yang tepat)
            if (jwtUtil.validateToken(jwt, username)) {
                
                // 5. Beri tahu Spring Security bahwa user ini BOLEH MASUK
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username, null, new ArrayList<>());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        
        // Lanjutkan request ke tujuan aslinya (misal: mengambil data produk)
        chain.doFilter(request, response);
    }
}