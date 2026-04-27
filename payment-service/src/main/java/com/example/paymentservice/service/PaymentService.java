package com.example.paymentservice.service;

import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.repository.PaymentRepository;
import com.example.paymentservice.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String TOPIC = "payment-topic";

    @Transactional
    public Payment processPayment(Long orderId, String method) {
        // 0. Cek apakah sudah ada pembayaran sukses untuk Order ini (Cegah bayar ganda)
        java.util.List<Payment> history = paymentRepository.findByOrderId(orderId);
        boolean alreadyPaid = history.stream().anyMatch(p -> "SUCCESS".equals(p.getStatus()));
        
        if (alreadyPaid) {
            throw new RuntimeException("Order ID " + orderId + " sudah dibayar sukses sebelumnya.");
        }

        // 1. Ambil data Order untuk mendapatkan customerId dan totalAmount
        String orderUrl = "http://localhost:8084/api/orders/" + orderId;
        HttpHeaders headers = new HttpHeaders();
        
        jakarta.servlet.http.HttpServletRequest currentRequest = 
            ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()).getRequest();
        String token = currentRequest.getHeader("Authorization");
        if (token != null) {
            headers.set("Authorization", token);
        }
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> orderResponse = restTemplate.exchange(orderUrl, HttpMethod.GET, entity, Map.class);
        Map orderData = (Map) orderResponse.getBody().get("data");
        Long customerId = Long.valueOf(orderData.get("customerId").toString());
        Double finalAmount = Double.valueOf(orderData.get("totalAmount").toString());

        // 2. Potong Saldo di Customer Service
        String deductUrl = "http://localhost:8083/api/customers/" + customerId + "/deduct-balance?amount=" + finalAmount;
        try {
            restTemplate.exchange(deductUrl, HttpMethod.PUT, entity, Map.class);
        } catch (Exception e) {
            // JIKA GAGAL (Misal saldo kurang), CATAT SEBAGAI FAILED
            saveAndNotifyFailed(orderId, finalAmount, method, "Gagal memotong saldo: " + e.getMessage());
            throw new RuntimeException("Pembayaran Gagal: Saldo tidak mencukupi atau masalah sistem.");
        }

        // 3. Simpan Transaksi Pembayaran SUKSES
        Payment payment = createPaymentRecord(orderId, finalAmount, method, "SUCCESS");
        Payment saved = paymentRepository.save(payment);

        // 4. Kirim Sinyal Sukses ke Kafka
        kafkaTemplate.send(TOPIC, orderId + ":SUCCESS");
        System.out.println("[PAYMENT SERVICE] Pembayaran SUKSES untuk Order " + orderId);

        return saved;
    }

    private void saveAndNotifyFailed(Long orderId, Double amount, String method, String reason) {
        Payment payment = createPaymentRecord(orderId, amount, method, "FAILED");
        paymentRepository.save(payment);
        kafkaTemplate.send(TOPIC, orderId + ":FAILED");
        System.err.println("[PAYMENT SERVICE] Pembayaran GAGAL untuk Order " + orderId + ". Alasan: " + reason);
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

    public java.util.List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Pembayaran dengan ID " + id + " tidak ditemukan"));
    }

    public java.util.List<Payment> getPaymentByOrderId(Long orderId) {
        java.util.List<Payment> payments = paymentRepository.findByOrderId(orderId);
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
}
