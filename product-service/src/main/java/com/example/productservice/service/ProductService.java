package com.example.productservice.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public Product updateProduct(Long id, Product request) {
        return productRepository.findById(id)
            .map(product -> {
                product.setName(request.getName()); 
                product.setDescription(request.getDescription());
                product.setImageUrl(request.getImageUrl());
                product.setPrice(request.getPrice()); 
                product.setStock(request.getStock()); 
                product.setCategory(request.getCategory()); 
                return productRepository.save(product); 
            }).orElse(null); 
    }

    public boolean deleteProduct(Long id) {
        if (productRepository.existsById(id)) { 
            productRepository.deleteById(id); 
            return true; 
        }
        return false; 
    }
    @org.springframework.transaction.annotation.Transactional
    public Product adjustStock(Long id, int amount) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produk tidak ditemukan"));
        
        int newStock = product.getStock() + amount;
        if (newStock < 0) {
            throw new RuntimeException("Stok tidak mencukupi untuk Produk: " + product.getName());
        }
        
        product.setStock(newStock);
        return productRepository.save(product);
    }
}
