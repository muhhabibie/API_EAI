package com.example.productservice.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.productservice.entity.Product;
import com.example.productservice.repository.ProductRepository;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll(); 
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id); 
    }

    public Product createProduct(Product product) {
        return productRepository.save(product); 
    }

    @Transactional
    public Product updateProduct(Long id, Product request) {
        // FIX #8: Throw exception alih-alih return null — lebih aman, tidak ada silent failure
        return productRepository.findById(id)
            .map(product -> {
                product.setName(request.getName());
                product.setDescription(request.getDescription());
                product.setImageUrl(request.getImageUrl());
                product.setPrice(request.getPrice());
                product.setStock(request.getStock());
                product.setCategory(request.getCategory());
                return productRepository.save(product);
            }).orElseThrow(() -> new RuntimeException("Produk dengan ID " + id + " tidak ditemukan"));
    }

    public boolean deleteProduct(Long id) {
        if (productRepository.existsById(id)) { 
            productRepository.deleteById(id); 
            return true; 
        }
        return false; 
    }
    @Transactional
    public Product adjustStock(Long id, int amount) {
        // FIX #3: Gunakan JPQL atomik — WHERE stock + amount >= 0 memastikan stok tidak negatif
        // bahkan jika dua Saga consumer memproses produk yang sama bersamaan.
        int updated = productRepository.adjustStockAtomic(id, amount);
        if (updated == 0) {
            Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produk tidak ditemukan: ID " + id));
            if (product.getStock() + amount < 0) {
                throw new RuntimeException("Stok tidak mencukupi untuk Produk: " + product.getName()
                    + " (stok=" + product.getStock() + ", diminta=" + Math.abs(amount) + ")");
            }
            throw new RuntimeException("Gagal menyesuaikan stok untuk Produk ID: " + id);
        }
        return productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Produk tidak ditemukan setelah adjustment: ID " + id));
    }
}
