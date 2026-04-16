package com.example.order_management.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.order_management.dto.ProductRequest;
import jakarta.validation.Valid;
import com.example.order_management.entity.Product;
import com.example.order_management.service.ProductService;
import com.example.order_management.repository.ProductRepository;
import org.springframework.web.server.ResponseStatusException;


@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/products") 
public class ProductController {
    @Autowired
    private ProductService productService;
    @Autowired
    private ProductRepository productRepository;
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts()); 
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok) 
                .orElseGet(() -> ResponseEntity.notFound().build()); 
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody @Valid ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        
        Product saved = productService.createProduct(product);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product product) {
    Product updated = productService.updateProduct(id, product);
    if (updated != null) {
        return ResponseEntity.ok(updated);
    }
    return ResponseEntity.notFound().build();
}


   @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        // Mengecek apakah produk ada sebelum dihapus
        boolean isDeleted = productService.deleteProduct(id);
        
        if (isDeleted) {
            // Status 204 No Content adalah standar pro untuk DELETE sukses
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}/adjustment")
    public ResponseEntity<Product> adjustStock(@PathVariable Long id, @RequestParam Integer amount) {
    // Menggunakan ResponseStatusException bawaan Spring
    Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produk tidak ditemukan"));
    
    int newStock = product.getStock() + amount;
    
    if (newStock < 0) {
        // Melempar error 400 Bad Request jika stok minus
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stok tidak boleh negatif!");
    }
    
    product.setStock(newStock);
    return ResponseEntity.ok(productRepository.save(product));
}
}