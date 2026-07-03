package com.example.productservice.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.productservice.entity.Product;
import com.example.productservice.kafka.KafkaTopics;
import com.example.productservice.repository.ProductRepository;
import com.example.saga.event.ProductStockSyncedEvent;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    // ── CRUD Produk ──────────────────────────────────────────────────────────

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    /**
     * Tambah produk baru.
     * Setelah disimpan, publish event product.stock.synced ke Inventory Service
     * agar tabel inventory langsung diinisialisasi dengan totalQty yang benar.
     */
    @Transactional
    public Product createProduct(Product product) {
        Product saved = productRepository.save(product);
        publishStockSynced(saved.getId(), saved.getStock(), "PRODUCT_UPDATED");
        return saved;
    }

    /**
     * Update data produk.
     * Jika stock berubah, publish event product.stock.synced ke Inventory Service.
     */
    @Transactional
    public Product updateProduct(Long id, Product request) {
        return productRepository.findById(id)
            .map(product -> {
                boolean stockChanged = !product.getStock().equals(request.getStock());
                product.setName(request.getName());
                product.setDescription(request.getDescription());
                product.setImageUrl(request.getImageUrl());
                product.setPrice(request.getPrice());
                product.setStock(request.getStock());
                product.setCategory(request.getCategory());
                Product saved = productRepository.save(product);

                if (stockChanged) {
                    publishStockSynced(saved.getId(), saved.getStock(), "PRODUCT_UPDATED");
                }
                return saved;
            }).orElseThrow(() -> new RuntimeException("Produk dengan ID " + id + " tidak ditemukan"));
    }

    public boolean deleteProduct(Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Penyesuaian stok manual oleh admin (+ atau -).
     * Setelah update, publish event product.stock.synced ke Inventory Service.
     */
    @Transactional
    public Product adjustStock(Long id, int amount) {
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
        Product saved = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Produk tidak ditemukan setelah adjustment: ID " + id));

        publishStockSynced(saved.getId(), saved.getStock(), "PRODUCT_UPDATED");
        return saved;
    }

    // ── Sinkronisasi dari Inventory Service ────────────────────────────────────

    /**
     * Dipanggil saat menerima event PAYMENT_CONFIRMED dari Inventory Service.
     * Langsung set stock ke nilai yang sudah dihitung Inventory (tanpa publish balik — hindari loop).
     */
    @Transactional
    public void syncStockFromInventory(Long productId, int newStock) {
        productRepository.findById(productId).ifPresentOrElse(product -> {
            product.setStock(newStock);
            productRepository.save(product);
            log.info("[PRODUCT-STOCK] ✓ Stock diperbarui dari Inventory | productId={} | stock={}", productId, newStock);
        }, () -> log.warn("[PRODUCT-STOCK] ⚠ Produk ID={} tidak ditemukan saat sync dari Inventory", productId));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void publishStockSynced(Long productId, int stock, String reason) {
        try {
            ProductStockSyncedEvent event = new ProductStockSyncedEvent(productId, stock, reason);
            kafkaTemplate.send(KafkaTopics.PRODUCT_STOCK_SYNCED, event);
            log.info("[PRODUCT-STOCK] ✓ Publish PRODUCT_STOCK_SYNCED | productId={} | stock={} | reason={}", productId, stock, reason);
        } catch (Exception e) {
            log.warn("[PRODUCT-STOCK] ⚠ Gagal publish PRODUCT_STOCK_SYNCED | productId={} | alasan={}", productId, e.getMessage());
        }
    }
}
