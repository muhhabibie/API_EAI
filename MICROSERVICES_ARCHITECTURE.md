# Diagram Arsitektur Microservices - API EAI Gateway

Dokumen ini mendokumentasikan visualisasi dan spesifikasi interaksi sistem **API EAI** yang menggunakan arsitektur microservices berbasis event (event-driven).

---

## 1. Diagram Visual Arsitektur

Berikut adalah diagram arsitektur sistem EAI Microservices yang menggambarkan aliran request sinkron (HTTP) dan asinkron (Kafka) serta pembagian database per service:

![Microservices Architecture Diagram](file:///C:/Users/Muhammad%20Habibi/.gemini/antigravity-ide/brain/cce8d992-96c9-4e9d-b27b-ca88f0a00f60/architecture_diagram_1780662332247.png)

---

## 2. Diagram Alir Komunikasi (Mermaid)

Di bawah ini adalah representasi diagram interaksi antar-layanan menggunakan Mermaid. Diagram ini membedakan hubungan sinkron (HTTP/REST dengan garis solid `-->`) dan asinkron (Kafka Events dengan garis putus-putus `-.->`):

```mermaid
graph TD
    classDef client fill:#f9f,stroke:#333,stroke-width:2px;
    classDef gateway fill:#bbf,stroke:#333,stroke-width:2px;
    classDef service fill:#fff,stroke:#333,stroke-width:2px;
    classDef broker fill:#f96,stroke:#333,stroke-width:2px;
    classDef db fill:#9f9,stroke:#333,stroke-width:2px;

    Client["User Client / Postman / Browser"]:::client
    Gateway["API Gateway (Port 8080)"]:::gateway

    subgraph Core Microservices
        Auth["Auth Service (Port 8081)"]:::service
        Product["Product Service (Port 8082)"]:::service
        Customer["Customer Service (Port 8083)"]:::service
        Order["Order Service (Port 8084)"]:::service
        Inventory["Inventory Service (Port 8085)"]:::service
        Shipping["Shipping Service (Port 8086)"]:::service
        Payment["Payment Service (Port 8087)"]:::service
    end

    subgraph Integration Broker
        Kafka["Apache Kafka 3.7.0 (Port 9092)"]:::broker
    end

    subgraph Distributed Databases (Local MySQL)
        AuthDB[("auth_db (MySQL)")]:::db
        ProductDB[("product_db (MySQL)")]:::db
        CustomerDB[("customer_db (MySQL)")]:::db
        OrderDB[("order_db (MySQL)")]:::db
        InventoryDB[("inventory_db (MySQL)")]:::db
        ShippingDB[("shipping_db (MySQL)")]:::db
        PaymentDB[("payment_db (MySQL)")]:::db
    end

    %% Client and Gateway Routing
    Client -->|1. HTTP Requests| Gateway
    Gateway -->|Proxy /api/auth| Auth
    Gateway -->|Proxy /api/products| Product
    Gateway -->|Proxy /api/customers| Customer
    Gateway -->|Proxy /api/orders| Order
    Gateway -->|Proxy /api/inventories| Inventory
    Gateway -->|Proxy /api/shipments| Shipping
    Gateway -->|Proxy /api/payments| Payment

    %% Database Isolation
    Auth --> AuthDB
    Product --> ProductDB
    Customer --> CustomerDB
    Order --> OrderDB
    Inventory --> InventoryDB
    Shipping --> ShippingDB
    Payment --> PaymentDB

    %% HTTP Synchronous calls (RestTemplate)
    Customer -->|Register User Credentials| Auth
    Payment -->|Deduct Wallet / Add Balance| Customer
    Payment -->|Query Order Total Amount| Order
    Order -->|Validate Product Metadata| Product
    Shipping -->|Validate Order Status PAID| Order
    Shipping -->|Fetch Customer Profile| Customer
    Inventory -->|Query Product Details| Product

    %% Asynchronous Kafka Event Flows (Saga Choreography)
    Order -.->|Publish order.created| Kafka
    Kafka -.->|Consume to Reserve Stock| Inventory
    
    Inventory -.->|Publish product.reserved| Kafka
    Kafka -.->|Consume to Process Payment| Payment
    
    Payment -.->|Publish payment.processed| Kafka
    Kafka -.->|Consume to Update Order status PAID| Order
    Kafka -.->|Consume to Commit Stock Reservation| Inventory
    
    %% Shipping status changes
    Shipping -.->|Publish order.shipped| Kafka
    Shipping -.->|Publish order.delivered| Kafka
    Kafka -.->|Consume to Update Order Status (SHIPPED/COMPLETED)| Order
```

---

## 3. Komponen Utama Arsitektur

### A. API Gateway (Port 8080)
Bertindak sebagai pintu gerbang tunggal (Single Entry Point) bagi semua request HTTP dari client (Web UI & Postman). Gateway melakukan proxying menuju port internal microservices yang sesuai secara dinamis.

### B. Core Microservices
Setiap layanan berjalan secara independen dan mengisolasi domain bisnisnya masing-masing:
* **Auth Service (8081)**: Mengelola user login, registrasi, enkripsi password, dan penerbitan token JWT.
* **Product Service (8082)**: Mengelola katalog produk, harga, dan kategori.
* **Customer Service (8083)**: Menyimpan profil pelanggan, alamat pengiriman, dan mengelola saldo dompet digital (wallet).
* **Order Service (8084)**: Mengorkestrasi pembuatan order, pembatalan pesanan, dan pencatatan riwayat transaksi lifecycle.
* **Inventory Service (8085)**: Bertanggung jawab atas ketersediaan stok, memproses penguncian stok sementara (reservation), dan melepaskan stok (compensation).
* **Shipping Service (8086)**: Menyimpan data pengiriman barang, resi, kurir, dan melacak manifest logistik.
* **Payment Service (8087)**: Mensimulasikan pemotongan saldo wallet dan memproses refund dana.

### C. Message Broker (Apache Kafka 3.7.0)
Menggunakan Kafka dalam mode KRaft (Zookeeper-less) sebagai tulang punggung integrasi data asinkron berbasis event (Saga Choreography Pattern). Ini menjamin bahwa jika salah satu service mati, transaksi terdistribusi tetap dapat dilanjutkan secara konsisten saat service kembali menyala.
