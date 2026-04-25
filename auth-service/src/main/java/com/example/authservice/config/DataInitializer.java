package com.example.authservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.authservice.entity.Customer;
import com.example.authservice.repository.CustomerRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Buat akun ADMIN default jika belum ada
        if (customerRepository.findByEmail("admin@example.com").isEmpty()) {
            Customer admin = new Customer();
            admin.setUsername("admin");
            admin.setName("Admin Utama");
            admin.setEmail("admin@example.com");
            admin.setAddress("Kantor Pusat");
            admin.setPassword(passwordEncoder.encode("admin123"));
            customerRepository.save(admin);
            System.out.println("✅ Akun ADMIN default dibuat: admin@example.com / admin123");
        } else {
            System.out.println("ℹ️  Akun ADMIN sudah ada, skip.");
        }

        // Buat akun USER default jika belum ada
        if (customerRepository.findByEmail("user@example.com").isEmpty()) {
            Customer user = new Customer();
            user.setUsername("user");
            user.setName("Regular User");
            user.setEmail("user@example.com");
            user.setAddress("Jalan User No. 1");
            user.setPassword(passwordEncoder.encode("user123"));
            customerRepository.save(user);
            System.out.println("✅ Akun USER default dibuat: user@example.com / user123");
        } else {
            System.out.println("ℹ️  Akun USER sudah ada, skip.");
        }
    }
}
