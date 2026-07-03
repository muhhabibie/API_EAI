package com.example.productservice.kafka;

import com.example.productservice.service.ProductService;
import com.example.saga.event.ProductStockSyncedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listener untuk event sinkronisasi stok dari Inventory Service.
 *
 * Dipicu saat pembayaran berhasil dikonfirmasi oleh Inventory Service,
 * sehingga kolom product.stock ikut berkurang sesuai qty yang terjual.
 */
@Component
public class ProductEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProductEventConsumer.class);

    @Autowired
    private ProductService productService;

    @KafkaListener(topics = KafkaTopics.PRODUCT_STOCK_SYNCED, groupId = "product-group")
    public void onProductStockSynced(ProductStockSyncedEvent event) {
        // Hanya proses event dari Inventory Service (setelah payment confirmed)
        if ("PAYMENT_CONFIRMED".equals(event.reason())) {
            log.info("[PRODUCT-STOCK] ► EVENT : PRODUCT_STOCK_SYNCED | productId={} | newStock={} | reason={}",
                    event.productId(), event.newStock(), event.reason());
            try {
                productService.syncStockFromInventory(event.productId(), event.newStock());
                log.info("[PRODUCT-STOCK] ✓ SUKSES : stock produk diperbarui | productId={} | stock={}",
                        event.productId(), event.newStock());
            } catch (Exception e) {
                log.error("[PRODUCT-STOCK] ✗ GAGAL  : Update stock produk | productId={} | alasan={}",
                        event.productId(), e.getMessage());
            }
        }
    }
}
