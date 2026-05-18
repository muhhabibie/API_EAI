# Saga Choreography — Sequence Diagrams

## 1. Happy Path (Alur Sukses Penuh)

```mermaid
sequenceDiagram
    actor Customer
    participant GW as API Gateway
    participant OS as Order Service
    participant IS as Inventory Service
    participant PS as Payment Service
    participant CS as Customer Service
    participant SS as Shipping Service
    participant K as Kafka

    Customer->>GW: POST /api/orders
    GW->>OS: Forward request

    OS->>OS: Validasi stok (sync via REST ke Product Service)
    OS->>OS: Buat Order (status: PENDING)
    OS->>K: Publish order.created

    K-->>IS: Consume ORDER_CREATED
    IS->>IS: Cek idempotency key
    IS->>IS: Reservasi stok (status: RESERVED)
    IS->>K: Publish product.reserved

    K-->>OS: Consume PRODUCT_RESERVED
    OS->>OS: Update Order (status: AWAITING_PAYMENT)

    Note over Customer,PS: ⏳ User melakukan pembayaran manual

    Customer->>GW: POST /api/payments (orderId, method, referenceNumber)
    GW->>PS: Forward request
    PS->>PS: Cek idempotency (referenceNumber)
    PS->>CS: Deduct balance (atomic query)
    CS-->>PS: Balance berhasil dipotong
    PS->>PS: Simpan record Payment (status: SUCCESS)
    PS->>K: Publish payment.processed

    K-->>OS: Consume PAYMENT_PROCESSED
    OS->>OS: Update Order (status: PAID)
    K-->>IS: Consume PAYMENT_PROCESSED
    IS->>IS: Konfirmasi reservasi (status: COMPLETED)

    Note over Customer,SS: ⏳ Admin membuat resi pengiriman manual

    Customer->>GW: POST /api/shipments (orderId, courier, ...)
    GW->>SS: Forward request
    SS->>OS: GET /api/orders/{id} — validasi status PAID
    OS-->>SS: Status = PAID ✅
    SS->>SS: Buat Shipment (status: PENDING)

    Note over Customer,SS: ⏳ Kurir mengambil paket (simulasi webhook)

    Customer->>GW: PUT /api/shipments/{id}/status?status=PICKED_UP
    GW->>SS: Forward request
    SS->>SS: Update Shipment (status: PICKED_UP)
    SS->>K: Publish order.shipped

    K-->>OS: Consume ORDER_SHIPPED
    OS->>OS: Update Order (status: SHIPPED)

    Note over Customer,SS: ⏳ Paket sampai ke tangan pembeli

    Customer->>GW: PUT /api/shipments/{id}/status?status=DELIVERED
    GW->>SS: Forward request
    SS->>SS: Update Shipment (status: DELIVERED)
    SS->>K: Publish order.delivered

    K-->>OS: Consume ORDER_DELIVERED
    OS->>OS: Update Order (status: COMPLETED)
    OS->>K: Publish order.completed

    Note over Customer,OS: ✅ SAGA SELESAI — Order COMPLETED
```

---

## 2. Compensation Path 1 — Saldo Tidak Cukup (Payment Failed)

```mermaid
sequenceDiagram
    actor Customer
    participant GW as API Gateway
    participant OS as Order Service
    participant IS as Inventory Service
    participant PS as Payment Service
    participant CS as Customer Service
    participant K as Kafka

    Customer->>GW: POST /api/orders
    GW->>OS: Forward request
    OS->>OS: Buat Order (status: PENDING)
    OS->>K: Publish order.created

    K-->>IS: Consume ORDER_CREATED
    IS->>IS: Reservasi stok (status: RESERVED)
    IS->>K: Publish product.reserved

    K-->>OS: Consume PRODUCT_RESERVED
    OS->>OS: Update Order (status: AWAITING_PAYMENT)

    Note over Customer,PS: ⏳ User mencoba bayar — tapi saldo tidak cukup!

    Customer->>GW: POST /api/payments (orderId, method)
    GW->>PS: Forward request
    PS->>CS: Deduct balance (atomic query)
    CS-->>PS: ❌ GAGAL — Saldo tidak cukup (balance < amount)
    PS->>K: Publish payment.failed

    K-->>OS: Consume PAYMENT_FAILED
    OS->>OS: Update Order (status: CANCELLED)
    OS->>K: Publish product.reservation.release

    K-->>IS: Consume RELEASE_RESERVATION
    IS->>IS: Lepas reservasi stok (status: RELEASED)
    IS->>IS: Kembalikan stok ke gudang ✅

    Note over Customer,IS: ✅ KOMPENSASI SELESAI — Stok dikembalikan, Order CANCELLED
```

---

## 3. Compensation Path 2 — Batal Setelah Bayar (Cancel + Refund)

```mermaid
sequenceDiagram
    actor Customer
    participant GW as API Gateway
    participant OS as Order Service
    participant IS as Inventory Service
    participant PS as Payment Service
    participant CS as Customer Service
    participant K as Kafka

    Note over Customer,OS: Order sudah PAID (pembayaran berhasil)

    Customer->>GW: PATCH /api/orders/{id}/cancel-after-payment
    GW->>OS: Forward request
    OS->>OS: Validasi: status harus PAID
    OS->>OS: Update Order (status: CANCELLED)

    par Kompensasi Paralel
        OS->>K: Publish product.reservation.release
    and
        OS->>K: Publish payment.refund
    end

    K-->>IS: Consume RELEASE_RESERVATION
    IS->>IS: Lepas reservasi stok (status: RELEASED)
    IS->>IS: Kembalikan stok ke gudang ✅

    K-->>PS: Consume PAYMENT_REFUND
    PS->>PS: Cari payment record by orderId (status: SUCCESS)
    PS->>CS: Add balance (kembalikan uang ke dompet Customer)
    CS-->>PS: Saldo berhasil dikembalikan ✅
    PS->>PS: Update Payment (status: REFUNDED)

    Note over Customer,CS: ✅ KOMPENSASI SELESAI — Stok kembali + Uang dikembalikan (Refund)
```

---

## 4. Compensation Path 3 — DLQ (Consumer Gagal Proses)

```mermaid
sequenceDiagram
    participant OS as Order Service
    participant IS as Inventory Service
    participant K as Kafka
    participant DLT as order.created.DLT

    OS->>K: Publish order.created

    K-->>IS: Consume ORDER_CREATED
    IS->>IS: ❌ ERROR (DB down / bug / koneksi terputus)

    Note over K,IS: Retry ke-1 (jeda 2 detik)
    K-->>IS: Retry ORDER_CREATED
    IS->>IS: ❌ ERROR

    Note over K,IS: Retry ke-2 (jeda 2 detik)
    K-->>IS: Retry ORDER_CREATED
    IS->>IS: ❌ ERROR

    Note over K,IS: Retry ke-3 (jeda 2 detik)
    K-->>IS: Retry ORDER_CREATED
    IS->>IS: ❌ ERROR — Menyerah setelah 3 retry

    IS->>DLT: Pindahkan pesan ke Karantina (DLT)
    IS->>IS: Log ERROR detail (Jenis Error, Penyebab, Offset)

    Note over DLT: Pesan aman tersimpan di DLT
    Note over DLT: DevOps bisa replay setelah masalah selesai
```
