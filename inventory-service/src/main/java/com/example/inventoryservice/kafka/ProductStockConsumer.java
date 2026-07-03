package com.example.inventoryservice.kafka;

import com.example.inventoryservice.service.InventoryService;
import com.example.saga.event.ProductStockSyncedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listener untuk event sinkronisasi stok dari Product Service.
 *
 * Dipicu saat admin menambah/update produk di Product Service,
 * sehingga Inventory Service selalu punya data totalQty yang sinkron.
 */
@Component
public class ProductStockConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProductStockConsumer.class);

    @Autowired
    private InventoryService inventoryService;

    @KafkaListener(topics = KafkaTopics.PRODUCT_STOCK_SYNCED, groupId = "inventory-group")
    public void onProductStockSynced(ProductStockSyncedEvent event) {
        // Hanya proses event dari Product Service (bukan yang dari diri sendiri setelah payment)
        if ("PRODUCT_UPDATED".equals(event.reason())) {
            log.info("[INVENTORY-STOCK] ► EVENT : PRODUCT_STOCK_SYNCED | productId={} | newStock={} | reason={}",
                    event.productId(), event.newStock(), event.reason());
            try {
                inventoryService.syncFromProduct(event.productId(), event.newStock());
                log.info("[INVENTORY-STOCK] ✓ SUKSES : Inventory tersinkronisasi | productId={} | totalQty={}",
                        event.productId(), event.newStock());
            } catch (Exception e) {
                log.error("[INVENTORY-STOCK] ✗ GAGAL  : Sync inventory | productId={} | alasan={}",
                        event.productId(), e.getMessage());
            }
        }
    }
}
