package com.example.productservice.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.productservice.dto.ApiResponse;
import com.example.productservice.dto.ProductRequest;
import com.example.productservice.entity.Product;
import com.example.productservice.entity.Category;
import com.example.productservice.service.ProductService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/products")
@Tag(name = "Product Management", description = "Endpoint untuk mengelola katalog produk")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Operation(summary = "Ambil Semua Produk", description = "Melihat seluruh daftar produk yang tersedia di katalog.")
    @GetMapping
    public ResponseEntity<?> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @Operation(summary = "Ambil Produk by ID", description = "Melihat detail informasi satu produk tertentu.")
    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(p -> ResponseEntity.ok(ApiResponse.success(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Tambah Produk Baru", description = "Menambahkan produk baru ke katalog. Khusus Admin.")
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> createProduct(@RequestBody ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setDescription(request.getDescription());
        
        if (request.getCategoryId() != null) {
            Category category = new Category();
            category.setId(request.getCategoryId());
            product.setCategory(category);
        }
        
        Product saved = productService.createProduct(product);
        return ResponseEntity.ok(ApiResponse.success("Produk berhasil ditambahkan", saved));
    }

    @Operation(summary = "Update Data Produk", description = "Memperbarui informasi harga, nama, atau deskripsi produk. Khusus Admin.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setDescription(request.getDescription());
        
        if (request.getCategoryId() != null) {
            Category category = new Category();
            category.setId(request.getCategoryId());
            product.setCategory(category);
        }
        
        Product updated = productService.updateProduct(id, product);
        return ResponseEntity.ok(ApiResponse.success("Produk berhasil diupdate", updated));
    }

    @Operation(summary = "Hapus Produk", description = "Menghapus produk dari katalog secara permanen. Khusus Admin.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Produk berhasil dihapus", null));
    }
    @Operation(summary = "Penyesuaian Stok Produk", description = "Menambah atau mengurangi stok produk secara manual. Khusus Admin.")
    @PostMapping("/{id}/adjustment")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> adjustStock(@PathVariable Long id, @RequestParam int amount) {
        Product updated = productService.adjustStock(id, amount);
        return ResponseEntity.ok(ApiResponse.success("Stok berhasil disesuaikan", updated));
    }
}
