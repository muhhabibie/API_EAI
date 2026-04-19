package com.example.order_management.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.order_management.entity.Product;
import com.example.order_management.repository.ProductRepository;

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
                product.setPrice(request.getPrice()); 
                product.setStock(request.getStock()); 
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

    // FUNGSI BARU: Untuk menambah atau mengurangi stok secara manual
    @org.springframework.transaction.annotation.Transactional
    public Product updateStock(Long productId, int amountToAdd) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produk dengan ID " + productId + " tidak ditemukan"));

        // Hitung stok baru (amountToAdd bisa bernilai negatif jika admin menginput minus)
        int newStock = product.getStock() + amountToAdd;
        
        // Validasi agar stok tidak menjadi minus di bawah 0
        if (newStock < 0) {
            throw new RuntimeException("Penyesuaian gagal: Stok akhir tidak boleh kurang dari 0.");
        }
        
        product.setStock(newStock);
        return productRepository.save(product);
    }
}