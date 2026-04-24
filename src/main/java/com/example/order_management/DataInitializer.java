package com.example.order_management;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.order_management.entity.Category;
import com.example.order_management.entity.Customer;
import com.example.order_management.entity.Product;
import com.example.order_management.repository.CategoryRepository;
import com.example.order_management.repository.CustomerRepository;
import com.example.order_management.repository.ProductRepository;

@Component
public class DataInitializer implements CommandLineRunner {
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;

    // Kita panggil PasswordEncoder untuk mengenkripsi password dummy kita
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 1. Inisialisasi Kategori
        Category electronics;
        Category accessories;
        
        if (categoryRepository.count() == 0) {
            electronics = categoryRepository.save(new Category("Electronics"));
            accessories = categoryRepository.save(new Category("Accessories"));
        } else {
            electronics = categoryRepository.findAll().get(0);
            accessories = categoryRepository.findAll().get(1);
        }

        // 2. Inisialisasi Produk
        if (productRepository.count() == 0) {
            Product laptop = new Product("Laptop", 8000000.0, 10);
            laptop.setCategory(electronics);
            productRepository.save(laptop);

            Product mouse = new Product("Mouse", 150000.0, 50);
            mouse.setCategory(accessories);
            productRepository.save(mouse);

            Product keyboard = new Product("Keyboard", 500000.0, 30);
            keyboard.setCategory(accessories);
            productRepository.save(keyboard);
        }

        // 3. Inisialisasi Pelanggan (SEKARANG MENGGUNAKAN PASSWORD)
        if (customerRepository.count() == 0) {
            // Kita buat password default "rahasia123" dan kita enkripsi
            String defaultPassword = passwordEncoder.encode("rahasia123");
            
            // Perhatikan bahwa sekarang ada 4 parameter yang dimasukkan
            customerRepository.save(new Customer("John Doe", "john@example.com", "123 Main St", defaultPassword));
            customerRepository.save(new Customer("Jane Smith", "jane@example.com", "456 Oak Ave", defaultPassword));
        }

        System.out.println("Data initialized successfully!");
        System.out.println("Categories: " + categoryRepository.count());
        System.out.println("Customers: " + customerRepository.count());
        System.out.println("Products: " + productRepository.count());
    }
}