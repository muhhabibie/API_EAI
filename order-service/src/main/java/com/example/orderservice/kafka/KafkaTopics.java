package com.example.orderservice.kafka;

public final class KafkaTopics {
    private KafkaTopics() {}

    public static final String ORDER_CREATED = "order.created";
    public static final String PRODUCT_RESERVED = "product.reserved";
    public static final String PRODUCT_RESERVATION_FAILED = "product.reservation.failed";
    public static final String PAYMENT_PROCESSED = "payment.processed";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String RELEASE_PRODUCT_RESERVATION = "product.reservation.release";
    public static final String REFUND_PAYMENT = "payment.refund";
    public static final String ORDER_COMPLETED = "order.completed";
    public static final String ORDER_CANCELLED = "order.cancelled";
}
