package com.example.paymentservice.service;

import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private com.example.paymentservice.security.JwtUtil jwtUtil;

    @Autowired
    private org.springframework.web.client.RestTemplate restTemplate;

    /**
     * Dipanggil dari Kafka Saga listener (onProductReserved).
     * Tidak bergantung pada HTTP request context — menggunakan System Token
     * dan menerima customerId + amount langsung dari event ProductReservedEvent.
     */
    @Transactional
    public void processPaymentFromSaga(String orderIdStr, Long customerId, java.math.BigDecimal amount) {
        Long orderId;
        try {
            orderId = Long.parseLong(orderIdStr);
        } catch (NumberFormatException e) {
            log.error("[PAYMENT-SAGA]  ✗ GAGAL  : orderId tidak valid      | orderId={}", orderIdStr);
            com.example.saga.event.PaymentFailedEvent failedEvent = new com.example.saga.event.PaymentFailedEvent(
                orderIdStr, amount, "orderId tidak valid: " + orderIdStr
            );
            kafkaTemplate.send(com.example.paymentservice.kafka.KafkaTopics.PAYMENT_FAILED, failedEvent);
            return;
        }

        // Idempotency: Cegah bayar ganda — cek SUCCESS dan PENDING
        // PENDING berarti payment sedang dalam proses, event duplikat harus ditolak
        List<Payment> history = paymentRepository.findByOrderId(orderId);
        boolean inProgress = history.stream()
            .anyMatch(p -> "SUCCESS".equals(p.getStatus()) || "PENDING".equals(p.getStatus()));
        if (inProgress) {
            log.warn("[PAYMENT-SAGA]  ⚠  DUPLIKAT: Payment sudah ada       | orderId={} | status={}",
                orderId, history.stream().map(Payment::getStatus).findFirst().orElse("?"));
            return;
        }

        log.info("[PAYMENT-SAGA]  ► Memproses pembayaran otomatis     | orderId={} | customerId={} | amount=Rp{}", orderId, customerId, amount);

        // FIX #1: Simpan record PENDING terlebih dahulu SEBELUM potong saldo.
        // Jika DB crash setelah saldo terpotong tapi sebelum save(), record ini
        // memastikan rekonsiliasi tetap bisa dilakukan (tidak ada saldo hilang tanpa jejak).
        Payment pendingPayment = createPaymentRecord(orderId, amount.doubleValue(), "BALANCE", "PENDING");
        Payment savedPending = paymentRepository.save(pendingPayment);
        log.info("[PAYMENT-SAGA]      record PENDING dibuat            | orderId={} | trxId={}", orderId, savedPending.getTransactionId());

        // Potong saldo customer menggunakan System Token (tanpa HTTP context)
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtUtil.generateSystemToken());
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String deductUrl = "http://localhost:8083/api/customers/" + customerId
                + "/deduct-balance?amount=" + amount.toPlainString();
            restTemplate.exchange(deductUrl, HttpMethod.PUT, entity, java.util.Map.class);
            log.info("[PAYMENT-SAGA]      saldo dipotong OK              | customerId={} | amount=Rp{}", customerId, amount);
        } catch (Exception e) {
            log.warn("[PAYMENT-SAGA]  ↩ KOMPENSASI: Saldo tidak cukup    | orderId={} | alasan={}", orderId, e.getMessage());
            // Update record PENDING → FAILED
            savedPending.setStatus("FAILED");
            paymentRepository.save(savedPending);
            com.example.saga.event.PaymentFailedEvent failedEvent = new com.example.saga.event.PaymentFailedEvent(
                String.valueOf(orderId), amount, "Gagal memotong saldo: " + e.getMessage()
            );
            kafkaTemplate.send(com.example.paymentservice.kafka.KafkaTopics.PAYMENT_FAILED, failedEvent);
            log.warn("[PAYMENT-SAGA]  ↩ KOMPENSASI: publish PAYMENT_FAILED   | orderId={} | alasan={}", orderId, e.getMessage());
            return;
        }

        // Update record PENDING → SUCCESS
        savedPending.setStatus("SUCCESS");
        Payment saved = paymentRepository.save(savedPending);

        // Publish payment.processed
        com.example.saga.event.PaymentProcessedEvent successEvent = new com.example.saga.event.PaymentProcessedEvent(
            String.valueOf(orderId), amount, saved.getTransactionId()
        );
        kafkaTemplate.send(com.example.paymentservice.kafka.KafkaTopics.PAYMENT_PROCESSED, successEvent);
        log.info("[PAYMENT-SAGA]  ✓ SUKSES : Pembayaran berhasil        | orderId={} | trxId={} → publish PAYMENT_PROCESSED", orderId, saved.getTransactionId());
    }

    @Transactional
    public Payment processPayment(Long orderId, String method) {
        List<Payment> history = paymentRepository.findByOrderId(orderId);
        boolean alreadyPaid = history.stream().anyMatch(p -> "SUCCESS".equals(p.getStatus()));
        if (alreadyPaid) {
            throw new RuntimeException("Order ID " + orderId + " sudah dibayar sukses sebelumnya.");
        }

        String orderUrl = "http://localhost:8084/api/orders/" + orderId;
        HttpHeaders headers = new HttpHeaders();
        jakarta.servlet.http.HttpServletRequest currentRequest =
            ((org.springframework.web.context.request.ServletRequestAttributes)
             org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()).getRequest();
        String token = currentRequest.getHeader("Authorization");
        if (token != null) {
            headers.set("Authorization", token);
        }
        HttpEntity<String> entity = new HttpEntity<>(headers);

        @SuppressWarnings("unchecked")
        ResponseEntity<Map<String, Object>> orderResponse = restTemplate.exchange(orderUrl, HttpMethod.GET, entity,
                (Class<Map<String, Object>>) (Class<?>) Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> orderData = (Map<String, Object>) orderResponse.getBody().get("data");
        Long customerId = Long.valueOf(orderData.get("customerId").toString());
        Double finalAmount = Double.valueOf(orderData.get("totalAmount").toString());

        // FIX #4: Simpan record PENDING sebelum potong saldo (pola yang sama dengan processPaymentFromSaga)
        Payment pendingPayment = createPaymentRecord(orderId, finalAmount, method, "PENDING");
        Payment savedPending = paymentRepository.save(pendingPayment);

        String deductUrl = "http://localhost:8083/api/customers/" + customerId + "/deduct-balance?amount=" + finalAmount;
        try {
            restTemplate.exchange(deductUrl, HttpMethod.PUT, entity, Map.class);
        } catch (Exception e) {
            savedPending.setStatus("FAILED");
            paymentRepository.save(savedPending);
            saveAndNotifyFailed(orderId, finalAmount, method, "Gagal memotong saldo: " + e.getMessage());
            throw new RuntimeException("Pembayaran Gagal: Saldo tidak mencukupi atau masalah sistem.");
        }

        // Update PENDING → SUCCESS
        savedPending.setStatus("SUCCESS");
        Payment saved = paymentRepository.save(savedPending);

        com.example.saga.event.PaymentProcessedEvent event = new com.example.saga.event.PaymentProcessedEvent(
            String.valueOf(orderId), java.math.BigDecimal.valueOf(finalAmount), saved.getTransactionId()
        );
        kafkaTemplate.send(com.example.paymentservice.kafka.KafkaTopics.PAYMENT_PROCESSED, event);
        log.info("[PAYMENT-SAGA]  ✓ SUKSES : Pembayaran manual berhasil  | orderId={} | trxId={}", orderId, saved.getTransactionId());
        return saved;
    }

    private void saveAndNotifyFailed(Long orderId, Double amount, String method, String reason) {
        Payment payment = createPaymentRecord(orderId, amount, method, "FAILED");
        paymentRepository.save(payment);
        com.example.saga.event.PaymentFailedEvent event = new com.example.saga.event.PaymentFailedEvent(
            String.valueOf(orderId), java.math.BigDecimal.valueOf(amount), reason
        );
        kafkaTemplate.send(com.example.paymentservice.kafka.KafkaTopics.PAYMENT_FAILED, event);
        log.warn("[PAYMENT-SAGA]  ↩ KOMPENSASI: publish PAYMENT_FAILED   | orderId={} | alasan={}", orderId, reason);
    }

    private Payment createPaymentRecord(Long orderId, Double amount, String method, String status) {
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setPaymentMethod(method);
        payment.setTransactionId("TRX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setStatus(status);
        payment.setPaymentDate(LocalDateTime.now());
        return payment;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Pembayaran dengan ID " + id + " tidak ditemukan"));
    }

    /**
     * Mengembalikan riwayat pembayaran yang sudah final (SUCCESS, FAILED, REFUNDED).
     * FIX isu #3: Tidak lagi mengembalikan record PENDING — mencegah client
     * salah mengira payment sudah selesai padahal masih diproses.
     * Jika hanya ada PENDING, throw exception agar client tahu payment sedang diproses.
     */
    public List<Payment> getPaymentByOrderId(Long orderId) {
        List<Payment> allPayments = paymentRepository.findByOrderId(orderId);
        if (allPayments.isEmpty()) {
            throw new RuntimeException("Riwayat pembayaran untuk Order ID " + orderId + " tidak ditemukan");
        }
        List<Payment> finalizedPayments = allPayments.stream()
            .filter(p -> !"PENDING".equals(p.getStatus()))
            .collect(java.util.stream.Collectors.toList());
        if (finalizedPayments.isEmpty()) {
            throw new RuntimeException("Pembayaran untuk Order ID " + orderId + " masih dalam proses (PENDING)");
        }
        return finalizedPayments;
    }

    /**
     * Mengembalikan SEMUA record pembayaran termasuk PENDING.
     * Digunakan untuk keperluan admin atau rekonsiliasi.
     */
    public List<Payment> getPaymentHistoryByOrderId(Long orderId) {
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        if (payments.isEmpty()) {
            throw new RuntimeException("Riwayat pembayaran untuk Order ID " + orderId + " tidak ditemukan");
        }
        return payments;
    }

    @Transactional
    public Payment updateStatus(Long id, String status) {
        Payment payment = getPaymentById(id);
        payment.setStatus(status.toUpperCase());
        return paymentRepository.save(payment);
    }

    @Transactional
    public void deletePayment(Long id) {
        Payment payment = getPaymentById(id);
        paymentRepository.delete(payment);
    }

    /**
     * Memproses refund untuk pembayaran berdasarkan Order ID.
     * Dipanggil oleh SagaEventListener (Kafka) — menggunakan System Token,
     * bukan RequestContextHolder (yang tidak tersedia di Kafka thread).
     */
    @Transactional
    public void refundByOrderId(Long orderId) {
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        boolean refunded = false;

        for (Payment payment : payments) {
            if ("SUCCESS".equals(payment.getStatus())) {
                payment.setStatus("REFUNDED");
                paymentRepository.save(payment);
                refunded = true;

                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Authorization", "Bearer " + jwtUtil.generateSystemToken());
                    HttpEntity<String> entity = new HttpEntity<>(headers);

                    // 1. Ambil customerId dari Order Service
                    String orderUrl = "http://localhost:8084/api/orders/" + orderId;
                    @SuppressWarnings("unchecked")
                    ResponseEntity<Map<String, Object>> orderResponse = restTemplate.exchange(
                        orderUrl, HttpMethod.GET, entity,
                        (Class<Map<String, Object>>) (Class<?>) Map.class
                    );
                    @SuppressWarnings("unchecked")
                    Map<String, Object> orderData = (Map<String, Object>) orderResponse.getBody().get("data");
                    Long customerId = Long.valueOf(orderData.get("customerId").toString());

                    // 2. Kembalikan saldo ke Customer Service
                    String addBalanceUrl = "http://localhost:8083/api/customers/" + customerId
                        + "/add-balance?amount=" + payment.getAmount();
                    restTemplate.exchange(addBalanceUrl, HttpMethod.PUT, entity, Map.class);

                    log.info("[PAYMENT-SAGA]  ✓ SUKSES : Saldo dikembalikan      | orderId={} | customerId={} | amount=Rp{} | trxId={}",
                        orderId, customerId, payment.getAmount(), payment.getTransactionId());

                } catch (Exception e) {
                    log.error("[PAYMENT-SAGA]  ✗ KRITIS : Gagal kembalikan saldo  | orderId={} | alasan={} → perlu reconciliation manual",
                        orderId, e.getMessage());
                }
            }
        }

        if (!refunded) {
            // FIX #10: Jangan throw exception — ini kondisi normal untuk safety-net refund
            // (misalnya: order dibatalkan saat AWAITING_PAYMENT sebelum payment sempat SUCCESS).
            // Throw di sini → Kafka listener retry → DLQ noise yang tidak perlu.
            log.warn("[PAYMENT-SAGA]  ⚠  SKIP   : Tidak ada SUCCESS payment  | orderId={} → tidak ada yang di-refund (expected for safety-net)", orderId);
        }
    }
}
