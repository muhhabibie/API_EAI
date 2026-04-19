# EAI Order Management API

Proyek ini adalah implementasi backend dan frontend untuk sistem manajemen order (Order Management) sebagai tugas mata kuliah **Enterprise Application Integration (EAI)**. Dibangun menggunakan **Spring Boot** (Java) dan **MySQL**, dengan frontend berbasis HTML/CSS/JS yang terintegrasi penuh dengan API.

## 👥 Kelompok
- Muhammad Habibi (235150201111063)
- Harry Phalosa Telaumbanua (235150200111052)
- Maulana Aryan Wicaksana Sabandar (235150201111056)
- Sindu Sanova (235150207111057)

## 🚀 Fitur yang Telah Diimplementasikan

### Backend (Spring Boot)
- **CRUD Products, Customers, Categories** dengan validasi input.
- **Order Management**: create order, get all orders, get order by id, update status, cancel order (dengan pengembalian stok, hanya untuk status PENDING).
- **Inventory API**: cek stok, reserve stok, release reservasi.
- **Shipping API**: create shipment, get shipment by order id, update status.
- **Global Exception Handler** untuk validasi dan error.
- **Database MySQL** dengan relasi antar entitas (Product, Customer, Order, OrderItem, Category, InventoryReservation, Shipment).

### Frontend – Admin Dashboard (`/admin/index.html`)
- **Create Transaction** (membuat order dengan pilihan customer, product, quantity).
- **Order History** (tabel riwayat order dengan tombol SHIP dan CANCEL).
- **CRUD Customers** (create, edit, delete) melalui panel khusus.
- **CRUD Products** (create, edit, delete, adjust stock) melalui panel khusus.
- **Inventory Panel** (cek stok, reserve stok, release reservasi).
- **API Inspector** (menampilkan request/response JSON dan cURL).
- **MySQL Table Viewer** (menampilkan isi tabel customers, products, orders secara real-time).

### Frontend – Halaman User / Marketplace (`/user/index.html`)
- **Login/Register sederhana** (pilih customer dari daftar yang sudah ada atau daftar customer baru).
- **Daftar produk** yang diambil langsung dari database (dinamis).
- **Keranjang belanja** berbasis `localStorage` (tambah, ubah jumlah, hapus item).
- **Checkout** (membuat order baru) dengan customer yang sedang login.
- **Riwayat order** (menampilkan daftar pesanan milik customer yang login, lengkap dengan detail item).
- **Shipping tracking** (menampilkan nomor resi dan status pengiriman untuk order yang sudah memiliki shipment).
- **Cancel order** (hanya untuk order dengan status PENDING, stok akan dikembalikan).

## 🛠️ Prasyarat

Sebelum menjalankan aplikasi, pastikan Anda telah menginstal:

- **Java 11 atau 17** (atau lebih tinggi)
- **Maven** (bisa menggunakan wrapper `mvnw` yang sudah disertakan)
- **MySQL Server 8.0** (atau lebih tinggi)
- **Git** (opsional, untuk clone repositori)

## 📦 Cara Menjalankan Aplikasi

### 1. Clone repositori (jika belum)
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

### Products
| Method | Endpoint | Deskripsi |
|--------|----------|-----------|
| GET | /api/products | Semua produk |
| GET | /api/products/{id} | Produk by ID |
| POST | /api/products | Buat produk baru |
| PUT | /api/products/{id} | Update produk |
| DELETE | /api/products/{id} | Hapus produk |
| PATCH | /api/products/{id}/adjustment | Tambah/kurang stok |

### Customers
| Method | Endpoint | Deskripsi |
|--------|----------|-----------|
| GET | /api/customers | Semua customer |
| GET | /api/customers/{id} | Customer by ID |
| POST | /api/customers | Buat customer baru |
| PUT | /api/customers/{id} | Update customer |
| DELETE | /api/customers/{id} | Hapus customer |

### Orders
| Method | Endpoint | Deskripsi |
|--------|----------|-----------|
| GET | /api/orders | Semua order |
| GET | /api/orders/{id} | Order by ID (dengan items) |
| POST | /api/orders?customerId={id} | Buat order baru (body: items) |
| PUT | /api/orders/{id}/status?status=xx | Update status order |
| POST | /api/orders/{id}/cancel | Batalkan order (kembalikan stok) |

### Categories
| Method | Endpoint | Deskripsi |
|--------|----------|-----------|
| GET | /api/categories | Semua kategori |
| GET | /api/categories/{id} | Kategori by ID |
| POST | /api/categories | Buat kategori baru |

### Inventory
| Method | Endpoint | Deskripsi |
|--------|----------|-----------|
| GET | /api/inventory/{productId} | Cek stok produk |
| POST | /api/inventory/reserve | Reserve stok (body: productId, quantity) |
| DELETE | /api/inventory/reserve/{id} | Release reservasi |

### Shipping
| Method | Endpoint | Deskripsi |
|--------|----------|-----------|
| POST | /api/shipments | Buat shipment baru (body: orderId, courierName) |
| GET | /api/shipments/{id} | Shipment by ID |
| GET | /api/shipments/order/{orderId} | Shipment by Order ID |
| PUT | /api/shipments/{id}/status?status=xx | Update status shipment |

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
```
