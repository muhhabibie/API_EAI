package com.example.productservice.kafka;

public final class KafkaTopics {
    private KafkaTopics() {}

    /** Dipublish Product Service → dikonsumsi Inventory Service */
    public static final String PRODUCT_STOCK_SYNCED = "product.stock.synced";
}
