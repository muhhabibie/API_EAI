# API EAI - Sistem Manajemen Pesanan dengan Arsitektur Microservices

---

## 1.1 Latar Belakang

Perkembangan teknologi digital telah mendorong transformasi signifikan pada berbagai sektor bisnis, khususnya dalam hal pengelolaan transaksi dan layanan pelanggan. Sistem manajemen pesanan (order management) menjadi salah satu komponen paling krusial dalam operasional perusahaan modern, mulai dari ritel, manufaktur, hingga penyedia jasa. Kemampuan untuk mencatat, memproses, dan memantau pesanan secara akurat dan efisien tidak hanya berdampak pada kepuasan pelanggan, tetapi juga secara langsung memengaruhi kesehatan arus kas dan pengelolaan inventori perusahaan.

Sayangnya, banyak pelaku usaha skala menengah dan kecil masih mengandalkan metode pencatatan manual atau sistem terpisah yang tidak saling terintegrasi. Akibatnya, muncul berbagai permasalahan seperti ketidaksesuaian data stok antar bagian, keterlambatan pemrosesan pesanan, serta kesulitan dalam melacak status pengiriman. Kondisi ini diperparah ketika perusahaan mulai mengembangkan kanal penjualan yang beragam—seperti toko fisik, situs web, atau aplikasi seluler—yang masing-masing membutuhkan sinkronisasi data secara real-time. Tanpa adanya sistem terpusat yang dapat diakses oleh berbagai aplikasi klien, potensi kesalahan manusia dan inkonsistensi data akan meningkat drastis.

Perkembangan sistem manajemen pesanan saat ini dituntut untuk memiliki skalabilitas dan fleksibilitas yang tinggi. Meskipun arsitektur monolitik lebih mudah diimplementasikan di awal, seiring bertambahnya beban transaksi, sistem tersebut seringkali menghadapi kendala dalam hal pemeliharaan dan ketergantungan antar-modul yang terlalu ketat. Permasalahan seperti ketidaksinkronan stok, kesulitan pelacakan status pengiriman, dan tidak responsifnya sistem terhadap beban pengguna yang tinggi menjadi alasan utama diperlukannya transformasi ke arsitektur yang lebih modern.

Proyek **API_EAI** secara spesifik telah mentransformasi sistem manajemen pesanan dari arsitektur monolitik menjadi **arsitektur microservices yang event-driven**. Dengan membagi fungsionalitas ke dalam **delapan layanan mandiri** (API Gateway, Auth, Product, Customer, Order, Inventory, Payment, dan Shipping), sistem dapat beroperasi secara independen dan terukur. Integrasi antar-layanan dilakukan melalui kombinasi protokol HTTP/REST untuk komunikasi sinkron dan **Apache Kafka 3.7.0** untuk komunikasi asinkron berbasis event. Selain itu, aspek keamanan diperketat dengan implementasi **JSON Web Token (JWT)** dan **Spring Security** untuk memastikan autentikasi yang aman dan efisien antar-layanan. Setiap layanan memiliki **database MySQL terpisah** untuk menjamin data isolation dan mendukung skalabilitas horizontal. Terakhir, seluruh spesifikasi endpoint didokumentasikan secara otomatis menggunakan standar **OpenAPI 3.0** (Swagger/Springdoc) guna menjamin interoperabilitas yang baik dan kemudahan integrasi bagi pengembang di masa mendatang.

---

## 1.2 Tujuan

Tujuan dari pengerjaan Proyek API_EAI adalah sebagai berikut:

1. **Menerapkan arsitektur microservices** menggunakan Spring Boot 4.0.5 dan Maven untuk membangun sistem manajemen pesanan yang terdesentralisasi dengan delapan layanan mandiri (API Gateway, Auth, Product, Customer, Order, Inventory, Payment, dan Shipping).

2. **Mengimplementasikan keamanan akses endpoint** menggunakan Spring Security dan JSON Web Token (JWT) pada Auth Service untuk memastikan autentikasi, otorisasi, dan proteksi resource endpoint di semua layanan.

3. **Mengelola komunikasi antar-layanan secara efisien** dengan:
   - Komunikasi sinkron menggunakan HTTP/REST untuk query dan validasi real-time
   - Komunikasi asinkron menggunakan Apache Kafka 3.7.0 untuk event streaming dan decoupling layanan
   - Implementasi Saga Choreography pattern untuk mengelola transaksi terdistribusi

4. **Mengimplementasikan entitas JPA dan relasinya secara terdistribusi** untuk modul Auth, Product, Customer, Order, Inventory, Payment, dan Shipping dengan menggunakan database MySQL terpisah per layanan.

5. **Mendokumentasikan seluruh spesifikasi teknis endpoint API** secara otomatis menggunakan standar OpenAPI 3.0 (Springdoc-openapi 3.0.3) dan Swagger UI untuk aksesibilitas dari browser.

6. **Menguji fungsionalitas sistem secara menyeluruh** melalui:
   - Integrasi event-driven menggunakan Kafka
   - Pengujian API menggunakan Postman dengan collection yang telah disediakan
   - Validasi alur transaksi end-to-end dari order creation hingga shipping

---

## 1.3 Ruang Lingkup

Agar pengerjaan proyek lebih terfokus, ruang lingkup API_EAI dibatasi sebagai berikut:

### 1.3.1 Fitur yang Diimplementasikan

**Tabel 1.1 - Fitur Microservices yang Diimplementasikan**

| No. | Fitur | Port | Database | Fungsi |
|-----|-------|------|----------|--------|
| 1 | **API Gateway** | 8080 | - | Request router centralized, load balancing, request distribution ke services yang sesuai |
| 2 | **Auth API** | 8081 | auth_db | Registrasi pengguna (BCrypt), login, manajemen token JWT, validasi credential |
| 3 | **Products API** | 8082 | product_db | CRUD data produk, pengelompokan berdasarkan kategori, manajemen harga produk |
| 4 | **Customer API** | 8083 | customer_db | Manajemen profil pelanggan, penyimpanan alamat pengiriman, manajemen balance/wallet |
| 5 | **Orders API** | 8084 | order_db | Pembuatan pesanan, konfirmasi pembayaran, manajemen status transaksi order |
| 6 | **Inventory API** | 8085 | inventory_db | Manajemen stok produk, reservasi stok otomatis, penyesuaian inventori |
| 7 | **Payment API** | 8087 | payment_db | Simulasi pemrosesan pembayaran, sinkronisasi status transaksi ke layanan terkait |
| 8 | **Shipping API** | 8086 | shipping_db | Pembuatan data pengiriman, integrasi kurir (simulasi), pelacakan status logistik |

    ### 1.3.2 Fitur Utama per Layanan

    #### API Gateway (Port 8080)
    - Centralized request routing ke services yang sesuai
    - Load balancing dan request distribution
    - Path-based routing untuk service discovery
    - Request/response logging dan monitoring
    - CORS (Cross-Origin Resource Sharing) handling
    - Unified entry point untuk semua client requests

    #### Auth Service
    - User registration dengan password hashing (BCrypt)
    - User login dengan email/password
    - JWT token generation dan validation
    - Role-based access control (RBAC)

    #### Product Service
    - CRUD (Create, Read, Update, Delete) produk
    - Manajemen kategori produk
    - Pengelompokan produk berdasarkan kategori
    - Price management per produk

    #### Customer Service
    - Profile management pelanggan
    - Penyimpanan alamat pengiriman
    - Manajemen balance/wallet pelanggan
    - Tracking history transaksi

    #### Order Service
    - Pembuatan order pesanan
    - Manajemen status order (PENDING → AWAITING_PAYMENT → PAID → SHIPPED → DELIVERED)
    - Konfirmasi pembayaran
    - Integrasi dengan Inventory Service untuk validasi stok
    - Event publishing untuk order lifecycle

    #### Inventory Service
    - Tracking stok produk real-time
    - Reservasi stok otomatis saat order dibuat
    - Release stok jika order dibatalkan
    - Penyesuaian stok saat pengiriman dikonfirmasi
    - Idempotency key handling untuk mencegah duplikasi

    #### Payment Service
    - Simulasi pemrosesan pembayaran
    - Deduction balance dari customer wallet
    - Recording payment transaction history
    - Event publishing untuk payment status
    - Idempotency support menggunakan reference number

    #### Shipping Service
    - Pembuatan resi pengiriman
    - Integrasi dengan mitra kurir (simulasi)
    - Tracking status pengiriman
    - Update order status berdasarkan shipping status

    ### 1.3.3 Arsitektur Teknologi

    #### Backend Stack
    - **Java Version**: 17
    - **Framework**: Spring Boot 4.0.5
    - **Build Tool**: Maven 3.9+
    - **Database**: MySQL 8.0+ (per service database: `auth_db`, `product_db`, `customer_db`, `order_db`, `inventory_db`, `shipping_db`, `payment_db` running on port 3306)
    - **Message Broker**: Apache Kafka 3.7.0 (KRaft Mode)
    - **API Documentation**: Springdoc-openapi 3.0.3 (OpenAPI/Swagger)
    - **Security**: Spring Security + JWT (JJWT 0.11.5)
    - **ORM**: Spring Data JPA

    #### Infrastructure & Monitoring
    - **Containerization**: Docker & Docker Compose
    - **Kafka UI**: `http://localhost:9000` (untuk monitoring topik dan consumer group Kafka)
    - **Observability Stack**:
      - **Jaeger (Distributed Tracing)**: `http://localhost:16686` (port OTLP gRPC 4317, HTTP 4318)
      - **Prometheus (Metrics Collection)**: `http://localhost:9090`
      - **Grafana (Visualization & Dashboards)**: `http://localhost:3001`
      - **Kafka Exporter**: `http://localhost:9308`
    - **Orchestration**: Docker Compose untuk deployment kafka & observability local development

    #### Komunikasi Antar-Layanan
    - **Sinkron (HTTP/REST via RestTemplate)**: 
      - `Customer Service` → `Auth Service` (`http://localhost:8081/api/register` untuk mendaftarkan kredensial login saat user register profil baru)
      - `Payment Service` → `Customer Service` (`http://localhost:8083/api/customers/.../deduct` untuk potong saldo dan `/api/customers/.../add-balance` untuk refund saldo)
      - `Payment Service` → `Order Service` (`http://localhost:8084/api/orders/...` untuk membaca data detail pesanan)
      - `Order Service` → `Product Service` (`http://localhost:8082/api/products/...` untuk memvalidasi detail barang)
      - `Shipping Service` → `Order Service` (`http://localhost:8084/api/orders/...` untuk memvalidasi status pembayaran PAID)
      - `Shipping Service` → `Customer Service` (`http://localhost:8083/api/customers/...` untuk mengambil profil data alamat penerima otomatis)
      - `Inventory Service` → `Product Service` (`http://localhost:8082/api/products/...` untuk validasi ketersediaan barang)
    - **Asinkron (Kafka Events)**:
      - **Order Service** →
        - Topic `order.created` : `OrderCreatedEvent` (memicu reservasi stok di Inventory Service)
        - Topic `product.reservation.release` : `ReleaseProductReservationEvent` (memicu pelepasan stok di Inventory Service jika pesanan batal)
        - Topic `payment.refund` : `RefundPaymentEvent` (memicu refund saldo di Payment Service jika order berstatus PAID dibatalkan)
      - **Inventory Service** →
        - Topic `product.reserved` : `ProductReservedEvent` (memicu pemotongan saldo di Payment Service)
        - Topic `product.reservation.failed` : `ProductReservationFailedEvent` (memicu pembatalan order di Order Service karena stok habis)
        - Topic `product.stock.synced` : `ProductStockSyncedEvent` (menyinkronkan stok fisik di database Product Service setelah pembayaran lunas)
      - **Payment Service** →
        - Topic `payment.processed` : `PaymentProcessedEvent` (memicu status order PAID di Order Service dan komitmen stok di Inventory Service)
        - Topic `payment.failed` : `PaymentFailedEvent` (memicu pembatalan order di Order Service karena saldo tidak cukup)
      - **Shipping Service** →
        - Topic `order.shipped` : `OrderShippedEvent` (mengubah status order menjadi SHIPPED di Order Service)
        - Topic `order.delivered` : `OrderDeliveredEvent` (mengubah status order menjadi COMPLETED di Order Service)

### 1.3.4 Batasan Proyek

1. Aplikasi dibangun menggunakan **arsitektur microservices dengan event-driven design**, bukan monolitik.

2. Komunikasi antar-layanan menggunakan **kombinasi protokol HTTP/REST** (untuk komunikasi sinkron) dan **Apache Kafka** (untuk event streaming asinkron).

3. Setiap microservice memiliki **database MySQL terpisah** untuk menjamin data isolation dan mendukung scalability horizontal.

4. **Teknologi utama** yang digunakan meliputi:
   - Java 17 + Spring Boot 4.0.5
   - Maven untuk dependency management
   - Docker & Docker Compose untuk deployment
   - MySQL 8.0 untuk persistence
   - Apache Kafka 3.7.0 untuk event streaming

5. Dokumentasi API tersedia melalui **Swagger UI / OpenAPI 3.0** yang dapat diakses langsung dari browser di endpoint `/swagger-ui.html` setiap service.

6. **Transaction Management** menggunakan **Saga Choreography Pattern** untuk mengelola distributed transactions:
   - Event-driven orchestration antar layanan
   - Idempotency key handling untuk mencegah duplikasi
   - Compensating transaction untuk rollback kegagalan

7. Tidak mencakup:
   - Integrasi dengan penyedia layanan pengiriman pihak ketiga secara real-time (menggunakan simulasi internal)
   - Payment gateway integrasi dengan pihak ketiga (menggunakan simulasi balance deduction)
   - UI frontend (fokus pada API backend)

### 1.3.5 Output Proyek

#### 1. Aplikasi Microservices
- **Delapan layanan mandiri** yang fully functional:
  - API Gateway (centralized routing)
  - Auth Service (JWT-based authentication)
  - Product Service (product & category management)
  - Customer Service (customer profile & balance)
  - Order Service (order orchestration)
  - Inventory Service (stock management)
  - Payment Service (payment processing)
  - Shipping Service (logistics management)

- **Event-driven architecture** menggunakan Apache Kafka 3.7.0 dengan:
  - Order event flow: OrderCreated → StockReserved → PaymentProcessed → ShipmentCreated
  - Proper error handling dan compensation mechanisms
  - Idempotency untuk mencegah duplicate processing

#### 2. Infrastruktur
- **Docker Compose setup** untuk orchestration semua services dan dependencies:
  - MySQL containers untuk masing-masing service database
  - Kafka & Kafka UI untuk event streaming dan monitoring
- **Database initialization scripts** (init.sql) untuk setup schema dan dummy data

#### 3. Dokumentasi
- **API Documentation**: Swagger UI accessible per service
- **Architecture Documentation**: 
  - MICROSERVICES_ARCHITECTURE.md - detailed architecture overview
  - KAFKA_ARCHITECTURE.md - event-driven communication patterns
  - SAGA_SEQUENCE_DIAGRAM.md - transaction flow visualization
- **Postman Collection**: API_EAI_Gateway.postman_collection.json untuk testing
- **Project README**: Comprehensive setup & running instructions

#### 4. Pengujian
- **Integration Testing**: End-to-end testing dari order creation hingga shipping
- **API Testing**: Postman collection untuk validasi semua endpoints
- **Event Testing**: Kafka message validation & consumer group testing
- **Saga Testing**: Distributed transaction flow verification

---

## 1.4 Kelompok Pengerjaan

| No. | Nama | NIM |
|-----|------|-----|
| 1 | Muhammad Habibi | 235150201111063 |
| 2 | Harry Phalosa Telaumbanua | 235150200111052 |
| 3 | Maulana Aryan Wicaksana Sabandar | 235150201111056 |
| 4 | Sindu Sanova | 235150207111057 |

---

## 1.5 Struktur Direktori Proyek

```
API_EAI/
├── api-gateway/                    # Port 8080 - Request Router & Load Balancer
├── auth-service/                   # Port 8081 - Authentication & JWT
├── product-service/                # Port 8082 - Product & Category Management
├── customer-service/               # Port 8083 - Customer Profile & Balance
├── order-service/                  # Port 8084 - Order Management & Orchestration
├── inventory-service/              # Port 8085 - Stock Management & Reservation
├── shipping-service/               # Port 8086 - Shipping & Tracking
├── payment-service/                # Port 8087 - Payment Processing Simulation
├── common-event/                   # Shared event models & DTOs
│
├── docker-compose.yml              # Main orchestration file
├── docker-compose.observability.yml # Observability & monitoring setup
├── init.sql                        # Database initialization scripts
├── pom.xml                         # Parent POM (Module aggregator)
├── README.md                       # Quick start guide
├── KAFKA_ARCHITECTURE.md           # Kafka events & topics documentation
├── SAGA_SEQUENCE_DIAGRAM.md        # Distributed transaction flows
├── API_EAI_Gateway.postman_collection.json # Postman test collection
│
├── start_services.sh               # Shell script to start all services
├── start_services.ps1              # PowerShell script to start all services
├── stop_services.sh                # Shell script to stop all services
└── stop_services.ps1               # PowerShell script to stop all services
```

---

## Referensi

- **Spring Boot Documentation**: https://spring.io/projects/spring-boot
- **Apache Kafka Documentation**: https://kafka.apache.org/documentation/
- **Springdoc-openapi**: https://springdoc.org/
- **JWT (JJWT)**: https://github.com/jwtk/jjwt
- **Docker Compose**: https://docs.docker.com/compose/
- **Microservices Patterns**: https://microservices.io/patterns/microservices.html
