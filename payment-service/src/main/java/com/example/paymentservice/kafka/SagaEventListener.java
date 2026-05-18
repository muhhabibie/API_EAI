package com.example.paymentservice.kafka;

import com.example.paymentservice.service.IdempotencyService;
import com.example.paymentservice.service.PaymentService;
import com.example.saga.event.ProductReservedEvent;
import com.example.saga.event.RefundPaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class SagaEventListener {

    private static final Logger log = LoggerFactory.getLogger(SagaEventListener.class);

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private IdempotencyService idempotencyService;

    /**
     * [SAGA - Step 2] Menerima konfirmasi bahwa stok berhasil direservasi.
     * Payment Service secara otomatis memproses pembayaran menggunakan saldo customer.
     * Jika berhasil → publish payment.processed
     * Jika gagal (saldo kurang) → publish payment.failed → kompensasi stok akan dipicu
     */
    @KafkaListener(topics = KafkaTopics.PRODUCT_RESERVED, groupId = "payment-service-group")
    public void onProductReserved(ProductReservedEvent event) {
        String eventKey = "PRODUCT_RESERVED_PAYMENT_" + event.orderId();
        if (idempotencyService.isAlreadyProcessed(eventKey)) {
            log.warn("[PAYMENT-SAGA]  ⚠  DUPLIKAT diabaikan  | event={}", eventKey);
            return;
        }
        log.info("[PAYMENT-SAGA]  ► EVENT : PRODUCT_RESERVED         | orderId={} | customerId={} | amount=Rp{}",
                event.orderId(), event.customerId(), event.amount());
        log.info("[PAYMENT-SAGA]  ► Menunggu pembayaran MANUAL dari user via endpoint POST /api/payments | orderId={}", event.orderId());
        // Auto-payment dinonaktifkan atas permintaan user. 
        // User harus memanggil POST /api/payments secara manual.
    }

    @KafkaListener(topics = KafkaTopics.REFUND_PAYMENT, groupId = "payment-service-group")
    public void onRefundPayment(RefundPaymentEvent event) {
        String eventKey = "REFUND_PAYMENT_" + event.orderId();
        if (idempotencyService.isAlreadyProcessed(eventKey)) {
            log.warn("[PAYMENT-SAGA]  ⚠  DUPLIKAT diabaikan  | event={}", eventKey);
            return;
        }
        log.info("[PAYMENT-SAGA]  ► EVENT : REFUND_PAYMENT            | orderId={} | alasan={}", event.orderId(), event.reason());
        try {
            Long orderId = Long.parseLong(event.orderId());
            paymentService.refundByOrderId(orderId);
        } catch (Exception e) {
            log.error("[PAYMENT-SAGA]  ✗ GAGAL  : Proses refund             | orderId={} | alasan={}", event.orderId(), e.getMessage());
        }
    }
}
