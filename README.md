# EAI Microservices Architecture

Proyek ini telah berhasil direfactor dari **monolithic architecture** menjadi **microservices architecture** dengan 6 services independen. Setiap service memiliki tanggung jawab domain-specific dan database terpisah.

## 👥 Kelompok
- Muhammad Habibi (235150201111063)
- Harry Phalosa Telaumbanua (235150200111052)
- Maulana Aryan Wicaksana Sabandar (235150201111056)
- Sindu Sanova (235150207111057)

## 📋 Project Structure

```
eai-microservices/
├── README.md                          # Dokumentasi utama (file ini)
├── MICROSERVICES_ARCHITECTURE.md      # Detailed architecture documentation
├── QUICK_START.md                     # Panduan quick start
├── pom.xml                            # Parent POM (Module aggregator)
├── docker-compose.yml                 # Container orchestration
├── init.sql                           # Database initialization script
│
├── auth-service/                      # Port 8081 - Authentication & JWT
├── product-service/                   # Port 8082 - Products & Categories
├── customer-service/                  # Port 8083 - Customer Management
├── order-service/                     # Port 8084 - Order Management
├── inventory-service/                 # Port 8085 - Stock Management
├── shipping-service/                  # Port 8086 - Shipping & Tracking
│
└── src/                               # Original monolithic code (to be archived)
```

## 🏗️ Microservices Overview

| Service | Port | Database | Responsibility |
|---------|------|----------|-----------------|
| **Auth Service** | 8081 | auth_db | User authentication & JWT tokens |
| **Product Service** | 8082 | product_db | Products & categories management |
| **Customer Service** | 8083 | customer_db | Customer profiles |
| **Order Service** | 8084 | order_db | Order creation & management |
| **Inventory Service** | 8085 | inventory_db | Stock reservations & management |
| **Shipping Service** | 8086 | shipping_db | Shipment tracking & courier management |

## 🚀 Quick Start

### Option 1: Run Locally (Recommended for Development)
```bash
# Build all services
mvn clean install

# Run each service in separate terminal
cd auth-service && mvn spring-boot:run
cd product-service && mvn spring-boot:run
cd customer-service && mvn spring-boot:run
cd order-service && mvn spring-boot:run
cd inventory-service && mvn spring-boot:run
cd shipping-service && mvn spring-boot:run
```

### Option 2: Run with Docker Compose
```bash
# Start all services with Docker
docker-compose up --build

# Stop services
docker-compose down
```

See [QUICK_START.md](./QUICK_START.md) for detailed instructions.

## 🛠️ Prerequisites

- **Java 17+**
- **Maven 3.9+**
- **MySQL 8.0+** (or use Docker)
- **Docker & Docker Compose** (optional, for containerized deployment)

## 📊 Service Details

### Auth Service (Port 8081)
**Endpoints**:
- `POST /api/login` - User login with email/password

**Database**: auth_db
- Customer table for authentication

### Product Service (Port 8082)
**Endpoints**:
- `GET /api/products` - List all products
- `POST /api/products` - Create product
- `PUT /api/products/{id}` - Update product
- `DELETE /api/products/{id}` - Delete product
- `GET /api/categories` - List categories
- `POST /api/categories` - Create category

**Database**: product_db
- Product & Category tables

### Customer Service (Port 8083)
**Endpoints**:
- `GET /api/customers` - List customers
- `POST /api/customers` - Create customer
- `PUT /api/customers/{id}` - Update customer
- `DELETE /api/customers/{id}` - Delete customer

**Database**: customer_db
- Customer table

### Order Service (Port 8084)
**Endpoints**:
- `POST /api/orders` - Create order
- `GET /api/orders` - List orders (with optional customerId filter)
- `PUT /api/orders/{id}/status` - Update order status
- `POST /api/orders/{id}/cancel` - Cancel order
- `POST /api/orders/{id}/pay` - Confirm payment

**Database**: order_db
- Order & OrderItem tables

### Inventory Service (Port 8085)
**Endpoints**:
- `POST /api/inventory/reservations` - Reserve stock
- `GET /api/inventory/reservations` - List all reservations
- `DELETE /api/inventory/reservations/{id}` - Release reservation

**Database**: inventory_db
- InventoryReservation table

### Shipping Service (Port 8086)
**Endpoints**:
- `POST /api/shipments` - Create shipment
- `GET /api/shipments` - List shipments
- `GET /api/shipments/{id}` - Get shipment by ID
- `GET /api/shipments/order/{orderId}` - Get shipment by order ID
- `PUT /api/shipments/{id}/status` - Update shipment status

**Database**: shipping_db
- Shipment table

## 🔐 Security

- **JWT Authentication**: Token-based authentication for API calls
- **Password Encryption**: BCrypt for secure password storage
- **CORS**: Enabled for frontend integration
- **Stateless Architecture**: Each service independently validates tokens

## 🗄️ Database Architecture

All services use independent MySQL databases:

```sql
-- Automatically created by Docker Compose or init.sql script
- auth_db (Customer)
- product_db (Product, Category)
- customer_db (Customer)
- order_db (Order, OrderItem)
- inventory_db (InventoryReservation)
- shipping_db (Shipment)
```

**Connection Details**:
- **Host**: localhost (or mysql service in Docker)
- **Port**: 3306
- **Username**: eai_user (or root for local)
- **Password**: W2yY021nH3r3

## 🧪 Testing APIs

### 1. Create Customer (Port 8083)
```bash
curl -X POST http://localhost:8083/api/customers \
  -H "Content-Type: application/json" \
  -d '{
    "name":"John Doe",
    "email":"john@example.com",
    "address":"123 Main Street",
    "password":"password123"
  }'
```

### 2. Login (Port 8081)
```bash
curl -X POST http://localhost:8081/api/login \
  -H "Content-Type: application/json" \
  -d '{
    "email":"john@example.com",
    "password":"password123"
  }'
```

### 3. Create Product (Port 8082)
```bash
curl -X POST http://localhost:8082/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name":"Laptop",
    "price":1299.99,
    "stock":50
  }'
```

### 4. Create Order (Port 8084)
```bash
curl -X POST "http://localhost:8084/api/orders?customerId=1" \
  -H "Content-Type: application/json" \
  -d '[{
    "productId":1,
    "quantity":2,
    "price":1299.99
  }]'
```

### 5. Create Shipment (Port 8086)
```bash
curl -X POST http://localhost:8086/api/shipments \
  -H "Content-Type: application/json" \
  -d '{
    "orderId":1,
    "courierName":"JNE"
  }'
```

## 📚 Documentation Files

- **[MICROSERVICES_ARCHITECTURE.md](./MICROSERVICES_ARCHITECTURE.md)** - Detailed architecture, service communication patterns, and database design
- **[QUICK_START.md](./QUICK_START.md)** - Step-by-step instructions for running services locally or with Docker
- **[DOCKERFILE_TEMPLATE.md](./DOCKERFILE_TEMPLATE.md)** - Docker setup and container configuration
- **[ORIGINAL_README.md](./ORIGINAL_README.md)** - Original monolithic application documentation

## 🔧 Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Java | 17+ |
| Framework | Spring Boot | 4.0.5 |
| Build Tool | Maven | 3.9+ |
| Database | MySQL | 8.0+ |
| Container | Docker | Latest |
| Authentication | JWT | 0.11.5 |
| API Docs | OpenAPI/Swagger | 2.5.0 |

## 🌐 API Endpoints Summary

### Auth Service (8081)
- `POST /api/login` - Login with credentials

### Product Service (8082)
- `GET /api/products`
- `POST /api/products`
- `PUT /api/products/{id}`
- `DELETE /api/products/{id}`
- `GET /api/categories`
- `POST /api/categories`

### Customer Service (8083)
- `GET /api/customers`
- `POST /api/customers`
- `PUT /api/customers/{id}`
- `DELETE /api/customers/{id}`

### Order Service (8084)
- `POST /api/orders`
- `GET /api/orders`
- `PUT /api/orders/{id}/status`
- `POST /api/orders/{id}/cancel`
- `POST /api/orders/{id}/pay`

### Inventory Service (8085)
- `POST /api/inventory/reservations`
- `GET /api/inventory/reservations`
- `DELETE /api/inventory/reservations/{id}`

### Shipping Service (8086)
- `POST /api/shipments`
- `GET /api/shipments`
- `GET /api/shipments/{id}`
- `GET /api/shipments/order/{orderId}`
- `PUT /api/shipments/{id}/status`

## 🚦 Running Individual Services

### Build Specific Service
```bash
mvn clean install -pl auth-service
```

### Run Specific Service
```bash
cd auth-service
mvn spring-boot:run

# OR after build
java -jar target/auth-service-0.0.1-SNAPSHOT.jar
```

## 🐳 Docker Deployment

### Build Docker Images
```bash
docker-compose build
```

### Start All Services
```bash
docker-compose up -d
```

### View Logs
```bash
docker-compose logs -f
docker-compose logs -f auth-service
```

### Stop All Services
```bash
docker-compose down
```

### Remove All Containers & Data
```bash
docker-compose down -v
```

## ⚠️ Important Notes

1. **Migration Status**: Old monolithic code in `/src` directory should be archived after microservices migration is complete
2. **Database Setup**: Use `init.sql` script or Docker Compose to automatically create databases
3. **Environment Variables**: Update `application.properties` for production deployment
4. **CORS Configuration**: Currently allows all origins (`*`). Restrict in production!
5. **Service Communication**: Currently uses REST. Consider async communication (Kafka/RabbitMQ) for production

## 🔮 Future Enhancements

- [ ] API Gateway (Kong/Spring Cloud Gateway)
- [ ] Service Discovery (Eureka/Consul)
- [ ] Distributed Tracing (Jaeger/Zipkin)
- [ ] Centralized Logging (ELK Stack)
- [ ] Message Queue (RabbitMQ/Kafka)
- [ ] Circuit Breaker (Hystrix/Resilience4j)
- [ ] Monitoring & Alerting (Prometheus/Grafana)
- [ ] Kubernetes Deployment
- [ ] API Versioning
- [ ] Performance Optimization

## 🤝 Contributing

1. Create feature branch
2. Make changes following the same structure pattern
3. Test locally
4. Submit pull request

## 📞 Troubleshooting

### Port Already in Use
```bash
# Find and kill process using port
lsof -i :8081
kill -9 <PID>

# OR change port in application.properties
server.port=9081
```

### MySQL Connection Error
```bash
# Check MySQL is running
mysql -u root -p

# Verify credentials in application.properties
spring.datasource.username=eai_user
spring.datasource.password=W2yY021nH3r3
```

### Docker Issues
```bash
# Clear Docker containers
docker-compose down -v

# Rebuild from scratch
docker-compose build --no-cache
docker-compose up
```

## 📄 License

This project is part of the Enterprise Application Integration (EAI) course.

---

**Last Updated**: April 25, 2026  
**Version**: 1.0 - Microservices Architecture
```bash
git clone https://github.com/muhhabibie/API_EAI.git
cd API_EAI
```

### 2. Siapkan database MySQL
Login ke MySQL:
```bash
mysql -u root -p
```
Buat database:
```sql
CREATE DATABASE IF NOT EXISTS order_management_db;
EXIT;
```

### 3. Konfigurasi `application.properties`
File terletak di `src/main/resources/application.properties`. Sesuaikan password MySQL Anda:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/order_management_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
server.port=8080
```

### 4. Jalankan aplikasi
```bash
.\mvnw spring-boot:run
```
Aplikasi akan berjalan di `http://localhost:8080`.

## 🌐 Cara Mengakses

- **Admin Dashboard**: `http://localhost:8080/admin/index.html`
- **Halaman User (Marketplace)**: `http://localhost:8080/user/index.html`
- **API Documentation (Swagger)**: `http://localhost:8080/swagger-ui/index.html` (jika berfungsi)

## 📡 API Endpoints

### Format Akses Endpoint API via Browser
Untuk mengakses endpoint API melalui browser, gunakan format berikut:

```
https://<base-url>/<endpoint>
```

Contoh:
- **GET** `/api/products` → `https://apieai-production-68fc.up.railway.app/api/products` (Rubah endpoint name paling belakang sesuai API yang dituju)
- **GET** `/api/products/{id}` → `https://apieai-production-68fc.up.railway.app/api/products/1` (akses produk dengan ID 1)
- **GET** `/api/orders/{id}` → `https://apieai-production-68fc.up.railway.app/api/orders/1` (akses order dengan ID 1)
- **GET** `/api/customers/{id}` → `https://apieai-production-68fc.up.railway.app/api/customers/1` (akses customer dengan ID 1)

### Products
| Method | Endpoint | Deskripsi |
|--------|----------|-----------|
| GET    | /api/products | Mendapatkan semua produk |
| GET    | /api/products/{id} | Mendapatkan produk berdasarkan ID |
| POST   | /api/products | Membuat produk baru |
| PUT    | /api/products/{id} | Memperbarui produk |
| DELETE | /api/products/{id} | Menghapus produk |

### Orders
| Method | Endpoint | Deskripsi |
|--------|----------|-----------|
| GET    | /api/orders | Mendapatkan semua order |
| POST   | /api/orders?customerId={id} | Membuat order baru |
| PUT    | /api/orders/{id}/status | Memperbarui status order |
| POST   | /api/orders/{id}/cancel | Membatalkan order |

### Customers
| Method | Endpoint | Deskripsi |
|--------|----------|-----------|
| GET    | /api/customers | Mendapatkan semua customer |
| GET    | /api/customers/{id} | Mendapatkan customer berdasarkan ID |
| POST   | /api/customers | Membuat customer baru |
| PUT    | /api/customers/{id} | Memperbarui customer |
| DELETE | /api/customers/{id} | Menghapus customer |

### Categories
| Method | Endpoint | Deskripsi |
|--------|----------|-----------|
| GET    | /api/categories | Mendapatkan semua kategori |
| GET    | /api/categories/{id} | Mendapatkan kategori berdasarkan ID |
| POST   | /api/categories | Membuat kategori baru |

### Inventory
| Method | Endpoint | Deskripsi |
|--------|----------|-----------|
| GET    | /api/inventory/{productId} | Mengecek stok produk |
| POST   | /api/inventory/reservations | Membuat reservasi stok |
| DELETE | /api/inventory/reservations/{id} | Membatalkan reservasi stok |

### Shipping
| Method | Endpoint | Deskripsi |
|--------|----------|-----------|
| POST   | /api/shipments | Membuat pengiriman baru |
| GET    | /api/shipments/{id} | Mendapatkan pengiriman berdasarkan ID |
| GET    | /api/shipments/order/{orderId} | Mendapatkan pengiriman berdasarkan ID order |
| PUT    | /api/shipments/{id}/status | Memperbarui status pengiriman |

## 🖥️ Tampilan Frontend

### Admin Dashboard (`/admin/index.html`)
- Sidebar dengan menu Orders, Products, Customers, Inventory.
- Form create transaction, tabel order history, CRUD produk/customer, inventory management, API inspector.

### Halaman User / Marketplace (`/user/index.html`)
- Hero section, kategori dekoratif, daftar produk dari database.
- Modal login/register (pilih customer atau daftar baru).
- Ikon keranjang dengan badge, modal keranjang (ubah jumlah, hapus, total).
- Tombol checkout, riwayat order dengan detail item, status, dan shipping tracking.
- Tombol cancel order (hanya untuk status PENDING).

## 📝 Catatan Penting

- Validasi input: harga produk tidak boleh negatif, email customer harus valid, stok tidak boleh negatif.
- Order dengan status `SHIPPED` atau `DELIVERED` **tidak dapat dibatalkan**.
- Cancel order akan mengembalikan stok produk.
- Endpoint DELETE pada product/customer akan gagal jika data masih terkait dengan order (foreign key constraint).
- Inventory API `reserve` langsung mengurangi stok; `release` mengembalikan stok.
- Shipping API memerlukan `courierName` yang valid (`JNE`, `JNT`, `POS`, `TIKI`, `SICEPAT`).

## 📚 Teknologi yang Digunakan

- Spring Boot 3.x
- Spring Data JPA
- MySQL Connector
- Validation API
- Maven
- HTML5, TailwindCSS, JavaScript (Vanilla)

## 📄 Lisensi

Dibuat untuk keperluan akademik Universitas Brawijaya – Mata Kuliah Enterprise Application Integration.

## Daftar Link untuk Mengakses URL Hostingnya

| Bagian         | Kode Markdown                                                                 |
|----------------|-------------------------------------------------------------------------------|
| Status Deploy  | ![Railway Deploy](https://img.shields.io/badge/Railway-Deployed-success?style=flat-square&logo=railway) |
| Link Admin     | [Laman Admin](https://apieai-production-68fc.up.railway.app/admin/index.html) - https://apieai-production-68fc.up.railway.app/admin/index.html|
| Link User      | [Laman User](https://apieai-production-68fc.up.railway.app/user/js/index.html) - https://apieai-production-68fc.up.railway.app/user/js/index.html |
