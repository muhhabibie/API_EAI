# Diagram Arsitektur: Evolusi Sistem (Monolitik -> As-Is Sebelum Broker -> As-Is Sekarang -> To-Be Target)

Dokumen ini memetakan empat fase evolusi arsitektur sistem **API EAI Gateway**:
1. **Arsitektur Monolitik (Kondisi Awal - Single DB & Memory Calls)**
2. **Arsitektur As-Is Sebelum Message Broker (Murni HTTP/REST Sinkron)**
3. **Arsitektur As-Is Sekarang (Hybrid Sync REST + Async Kafka)**
4. **Arsitektur To-Be Target (Decoupled Event-Driven + Service Discovery)**

---

## 1. Arsitektur Monolitik (Kondisi Awal)

Pada kondisi awal (sebelum refactoring), seluruh komponen domain (Auth, Product, Customer, Order, Inventory, Shipping, dan Payment) digabungkan dalam **satu aplikasi Spring Boot tunggal** yang dideploy sebagai satu unit (Single Deployable Artifact) pada satu port.

### Karakteristik:
- **In-Memory Calls**: Pertukaran data antar-domain dilakukan langsung di memori menggunakan pemanggilan method Java (direct method call / dependency injection).
- **Single Database**: Semua tabel (`users`, `products`, `orders`, `inventory`, dll.) berada dalam satu database yang sama (`order_management_db`), memungkinkan kueri gabungan (JOIN) dan transaksi lokal (ACID) secara langsung.
- **Kemudahan Deployment**: Sangat mudah dideploy karena hanya ada satu file jar/war, namun sulit dikembangkan secara paralel dan rentan terhadap beban komputasi terpusat.

### Diagram Arsitektur (Monolitik):

```mermaid
graph TD
    classDef client fill:#e1f5fe,stroke:#0288d1,stroke-width:2px;
    classDef monolith fill:#fff3e0,stroke:#ffb74d,stroke-width:2px;
    classDef component fill:#fffde7,stroke:#fbc02d,stroke-width:2px;
    classDef db fill:#ede7f6,stroke:#5e35b1,stroke-width:2px;

    Client["User Client / Browser / Postman"]:::client

    subgraph Monolithic Application (Single Port 8080)
        direction TB
        App["Spring Boot Monolith Application (JAR/WAR)"]:::monolith
        
        subgraph Internal Modules / Packages
            AuthMod["Auth Module"]:::component
            ProductMod["Product Module"]:::component
            CustomerMod["Customer Module"]:::component
            OrderMod["Order Module"]:::component
            InventoryMod["Inventory Module"]:::component
            ShippingMod["Shipping Module"]:::component
            PaymentMod["Payment Module"]:::component
        end
    end

    subgraph Monolithic Database
        MonoDB[("Single Database (order_management_db)<br/>- users & roles<br/>- products & categories<br/>- customers<br/>- orders & order_items<br/>- inventory & reservations<br/>- shipments<br/>- payments")]:::db
    end

    %% Client Routing
    Client -->|HTTP Requests| App
    
    %% Java Method Calls (In-Memory Inter-module Communication)
    OrderMod -->|Call Java Method| ProductMod
    OrderMod -->|Call Java Method| InventoryMod
    PaymentMod -->|Call Java Method| CustomerMod
    PaymentMod -->|Call Java Method| OrderMod
    ShippingMod -->|Call Java Method| OrderMod
    ShippingMod -->|Call Java Method| CustomerMod
    CustomerMod -->|Call Java Method| AuthMod
    
    %% Database Access (Local Transactions)
    AuthMod --> MonoDB
    ProductMod --> MonoDB
    CustomerMod --> MonoDB
    OrderMod --> MonoDB
    InventoryMod --> MonoDB
    ShippingMod --> MonoDB
    PaymentMod --> MonoDB
```

---

## 2. Arsitektur "As-Is" Sebelum Message Broker (Murni HTTP/REST Sinkron)

Setelah sistem dimigrasikan pertama kali ke microservices, aplikasi dipecah menjadi 7-8 service terpisah, masing-masing dengan database MySQL tersendiri (*Database-per-Service*). Namun, koordinasi transaksinya menggunakan pemanggilan **HTTP/REST API secara sinkron** (menggunakan `RestTemplate`).

### Karakteristik & Masalah:
- **Tight Coupling (Keterikatan Kuat)**: Setiap service bergantung langsung pada ketersediaan runtime service lainnya di jaringan.
- **Cascading Failure**: Jika `Product Service` mati, proses pembuatan pesanan di `Order Service` dan `Inventory Service` langsung gagal.
- **Latensi Tinggi**: Karena pemanggilan API dilakukan berantai (serial), latensi bertambah di sisi client.

### Diagram Arsitektur (Murni Sinkron):

```mermaid
graph TD
    classDef client fill:#e1f5fe,stroke:#0288d1,stroke-width:2px;
    classDef gateway fill:#e8f5e9,stroke:#2e7d32,stroke-width:2px;
    classDef service fill:#fffde7,stroke:#fbc02d,stroke-width:2px;
    classDef db fill:#ede7f6,stroke:#5e35b1,stroke-width:2px;

    Client["User Client / Browser / Postman"]:::client
    Gateway["API Gateway (Port 8080)<br/>(ProxyController)"]:::gateway

    subgraph Core Services (Ports: 8081-8087)
        Auth["Auth Service (8081)"]:::service
        Product["Product Service (8082)"]:::service
        Customer["Customer Service (8083)"]:::service
        Order["Order Service (8084)"]:::service
        Inventory["Inventory Service (8085)"]:::service
        Shipping["Shipping Service (8086)"]:::service
        Payment["Payment Service (8087)"]:::service
    end

    subgraph Databases (MySQL 8.0)
        AuthDB[("auth_db")]:::db
        ProductDB[("product_db")]:::db
        CustomerDB[("customer_db")]:::db
        OrderDB[("order_db")]:::db
        InventoryDB[("inventory_db")]:::db
        ShippingDB[("shipping_db")]:::db
        PaymentDB[("payment_db")]:::db
    end

    %% Client Routing
    Client -->|HTTP Requests| Gateway
    Gateway -->|Static HTTP Proxy| Auth
    Gateway -->|Static HTTP Proxy| Product
    Gateway -->|Static HTTP Proxy| Customer
    Gateway -->|Static HTTP Proxy| Order
    Gateway -->|Static HTTP Proxy| Inventory
    Gateway -->|Static HTTP Proxy| Shipping
    Gateway -->|Static HTTP Proxy| Payment

    %% DB Connection
    Auth --> AuthDB
    Product --> ProductDB
    Customer --> CustomerDB
    Order --> OrderDB
    Inventory --> InventoryDB
    Shipping --> ShippingDB
    Payment --> PaymentDB

    %% Murni HTTP Synchronous calls (Coupled REST)
    Customer -->|1. Register Credentials (Sync)| Auth
    Order -->|2. Validate Product Metadata (Sync)| Product
    Order -->|3. Reserve Stock (Sync)| Inventory
    Inventory -->|4. Query Product Details (Sync)| Product
    Payment -->|5. Deduct Wallet Balance (Sync)| Customer
    Payment -->|6. Query Order Amount (Sync)| Order
    Payment -->|7. Update Order Status PAID (Sync)| Order
    Shipping -->|8. Validate Order status PAID (Sync)| Order
    Shipping -->|9. Fetch Customer Address (Sync)| Customer
    Shipping -->|10. Update Order status SHIPPED/DELIVERED (Sync)| Order
```

---

## 3. Arsitektur "As-Is" Sekarang (Hybrid Sync REST + Async Kafka)

Pada arsitektur saat ini, sistem telah ditransformasikan secara parsial dengan memasukkan **Apache Kafka** untuk mengorkestrasi transaksi terdistribusi menggunakan **Saga Choreography Pattern**. Namun, beberapa pemanggilan query data statis/referensi masih dilakukan secara sinkron melalui HTTP REST.

### Diagram Arsitektur (Hybrid):

```mermaid
graph TD
    classDef client fill:#e1f5fe,stroke:#0288d1,stroke-width:2px;
    classDef gateway fill:#e8f5e9,stroke:#2e7d32,stroke-width:2px;
    classDef service fill:#fffde7,stroke:#fbc02d,stroke-width:2px;
    classDef broker fill:#ffe0b2,stroke:#f57c00,stroke-width:2px;
    classDef db fill:#ede7f6,stroke:#5e35b1,stroke-width:2px;

    Client["User Client / Browser / Postman"]:::client
    Gateway["Custom API Gateway (Port 8080)<br/>(ProxyController)"]:::gateway

    subgraph Core Services (Ports: 8081-8087)
        Auth["Auth Service (8081)"]:::service
        Product["Product Service (8082)"]:::service
        Customer["Customer Service (8083)"]:::service
        Order["Order Service (8084)"]:::service
        Inventory["Inventory Service (8085)"]:::service
        Shipping["Shipping Service (8086)"]:::service
        Payment["Payment Service (8087)"]:::service
    end

    subgraph Databases (MySQL 8.0)
        AuthDB[("auth_db")]:::db
        ProductDB[("product_db")]:::db
        CustomerDB[("customer_db")]:::db
        OrderDB[("order_db")]:::db
        InventoryDB[("inventory_db")]:::db
        ShippingDB[("shipping_db")]:::db
        PaymentDB[("payment_db")]:::db
    end

    subgraph Message Broker
        Kafka["Apache Kafka 3.7.0 (9092)"]:::broker
    end

    %% Client Routing
    Client -->|HTTP Requests| Gateway
    Gateway -->|Static HTTP Proxy| Auth
    Gateway -->|Static HTTP Proxy| Product
    Gateway -->|Static HTTP Proxy| Customer
    Gateway -->|Static HTTP Proxy| Order
    Gateway -->|Static HTTP Proxy| Inventory
    Gateway -->|Static HTTP Proxy| Shipping
    Gateway -->|Static HTTP Proxy| Payment

    %% DB Connection
    Auth --> AuthDB
    Product --> ProductDB
    Customer --> CustomerDB
    Order --> OrderDB
    Inventory --> InventoryDB
    Shipping --> ShippingDB
    Payment --> PaymentDB

    %% HTTP Synchronous calls (Coupled REST)
    Customer -->|Register Credentials (Sync REST)| Auth
    Order -->|Validate Product (Sync REST)| Product
    Inventory -->|Query Product (Sync REST)| Product
    Payment -->|Deduct Wallet Balance (Sync REST)| Customer
    Payment -->|Query Order Total (Sync REST)| Order
    Shipping -->|Validate Order Status (Sync REST)| Order
    Shipping -->|Fetch Customer Profile (Sync REST)| Customer

    %% Asynchronous Saga Events via Kafka
    Order -.->|Publish: order.created| Kafka
    Kafka -.->|Consume| Inventory
    
    Inventory -.->|Publish: product.reserved| Kafka
    Kafka -.->|Consume| Payment
    Kafka -.->|Consume| Order
    
    Payment -.->|Publish: payment.processed| Kafka
    Kafka -.->|Consume| Order
    Kafka -.->|Consume| Inventory
    
    Shipping -.->|Publish: order.shipped / delivered| Kafka
    Kafka -.->|Consume| Order
```

---

## 4. Arsitektur "To-Be" (Target Usulan)

Arsitektur target menghilangkan seluruh pemanggilan sinkron runtime antar-layanan menggunakan **Data Replication (Eventual Consistency via Kafka)**, mengimplementasikan **Eureka Service Discovery**, serta memusatkan verifikasi JWT (**Edge Auth**) di API Gateway.

### Diagram Arsitektur (Target):

```mermaid
graph TD
    classDef client fill:#e1f5fe,stroke:#0288d1,stroke-width:2px;
    classDef gateway fill:#e8f5e9,stroke:#2e7d32,stroke-width:2px;
    classDef service fill:#fffde7,stroke:#fbc02d,stroke-width:2px;
    classDef broker fill:#ffe0b2,stroke:#f57c00,stroke-width:2px;
    classDef db fill:#ede7f6,stroke:#5e35b1,stroke-width:2px;
    classDef registry fill:#f3e5f5,stroke:#ab47bc,stroke-width:2px;

    Client["User Client / Browser / Postman"]:::client
    
    subgraph Edge API Gateway
        Gateway["Spring Cloud Gateway (8080)<br/>- Centralized JWT Validation<br/>- Inject Identity Headers (X-User-* )<br/>- Dynamic Routing"]:::gateway
    end

    subgraph Service Registry
        Eureka["Eureka Discovery Server (8761)"]:::registry
    end

    subgraph Core Decoupled Services
        Auth["Auth Service<br/>(Dynamic Port)"]:::service
        Product["Product Service<br/>(Dynamic Port)"]:::service
        Customer["Customer Service<br/>(Dynamic Port)"]:::service
        Order["Order Service<br/>(Dynamic Port)"]:::service
        Inventory["Inventory Service<br/>(Dynamic Port)"]:::service
        Shipping["Shipping Service<br/>(Dynamic Port)"]:::service
        Payment["Payment Service<br/>(Dynamic Port)"]:::service
    end

    subgraph Databases (MySQL 8.0)
        AuthDB[("auth_db")]:::db
        ProductDB[("product_db")]:::db
        CustomerDB[("customer_db")]:::db
        OrderDB[("order_db <br/> + local_product_cache")]:::db
        InventoryDB[("inventory_db <br/> + local_product_cache")]:::db
        ShippingDB[("shipping_db <br/> + local_customer_cache")]:::db
        PaymentDB[("payment_db")]:::db
    end

    subgraph Message Broker
        Kafka["Apache Kafka Cluster"]:::broker
    end

    %% Eureka Registration
    Gateway -.->|Query Instances| Eureka
    Auth -.->|Register| Eureka
    Product -.->|Register| Eureka
    Customer -.->|Register| Eureka
    Order -.->|Register| Eureka
    Inventory -.->|Register| Eureka
    Shipping -.->|Register| Eureka
    Payment -.->|Register| Eureka

    %% Client Routing Flow
    Client -->|1. Request + JWT| Gateway
    Gateway -->|2. Route Dynamically via Eureka| Order
    Gateway -->|3. Route Dynamically via Eureka| Customer

    %% DB Connectivity
    Auth --> AuthDB
    Product --> ProductDB
    Customer --> CustomerDB
    Order --> OrderDB
    Inventory --> InventoryDB
    Shipping --> ShippingDB
    Payment --> PaymentDB

    %% Event-Driven Replication (Decoupling)
    Product -.->|Publish: product.created/updated/deleted| Kafka
    Kafka -.->|Consume & Sync Local Cache| Order
    Kafka -.->|Consume & Sync Local Cache| Inventory

    Customer -.->|Publish: customer.registered| Kafka
    Kafka -.->|Async Credentials Setup| Auth
    Kafka -.->|Consume & Sync Address| Shipping

    %% Fully Event-Driven Saga Payments
    Inventory -.->|Publish: product.reserved| Kafka
    Kafka -.->|Process Payment Async| Payment
    Payment -.->|Publish: payment.processed| Kafka
    Kafka -.->|Consume & Deduct Balance| Customer
    Kafka -.->|Acknowledge Status PAID| Order
    Kafka -.->|Commit Stock Reservation| Inventory
```
