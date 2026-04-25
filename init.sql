-- Create databases
CREATE DATABASE IF NOT EXISTS auth_db;
CREATE DATABASE IF NOT EXISTS product_db;
CREATE DATABASE IF NOT EXISTS customer_db;
CREATE DATABASE IF NOT EXISTS order_db;
CREATE DATABASE IF NOT EXISTS inventory_db;
CREATE DATABASE IF NOT EXISTS shipping_db;

-- Grant privileges to root
GRANT ALL PRIVILEGES ON auth_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON product_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON customer_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON order_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON inventory_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON shipping_db.* TO 'root'@'%';

FLUSH PRIVILEGES;

-- Use auth_db
USE auth_db;

-- Create customers table
CREATE TABLE IF NOT EXISTS customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    address VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert sample user (password: admin123)
-- BCrypt hash of "admin123": $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcg7b3XeKeUxWdeS86E36DxYYYm
INSERT INTO customers (username, email, password, name, address) VALUES 
('admin', 'admin@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcg7b3XeKeUxWdeS86E36DxYYYm', 'Admin User', 'Jl. Merdeka No. 1, Jakarta'),
('user', 'user@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcg7b3XeKeUxWdeS86E36DxYYYm', 'Regular User', 'Jl. Sudirman No. 10, Bandung')
ON DUPLICATE KEY UPDATE email=email;

-- Use product_db
USE product_db;

-- Create categories table
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT
);

-- Create products table
CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(19,2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    category_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- Insert sample categories
INSERT INTO categories (name, description) VALUES 
('Makanan', 'Produk makanan dan minuman'),
('Kerajinan', 'Produk kerajinan tangan'),
('Fashion', 'Produk fashion dan pakaian'),
('Elektronik', 'Produk elektronik dan gadget')
ON DUPLICATE KEY UPDATE name=name;

-- Insert sample products
INSERT INTO products (name, description, price, stock, category_id) VALUES 
('Kopi Arabika Premium', 'Kopi arabika pilihan dari Sumatra', 150000, 50, 1),
('Batik Tradisional', 'Batik asli dengan motif tradisional', 250000, 30, 3),
('Tas Rajut', 'Tas tangan rajut dengan desain modern', 180000, 25, 3),
('Keramik Buatan Tangan', 'Keramik unik hasil karya pengrajin lokal', 320000, 15, 2),
('Minyak Kelapa Organik', 'Minyak kelapa murni tanpa bahan kimia', 120000, 40, 1),
('Tas Kulit Asli', 'Tas kulit berkualitas tinggi dengan jahitan rapi', 450000, 20, 3);

-- Use customer_db
USE customer_db;

-- Create customers table (duplicate from auth_db for reference)
CREATE TABLE IF NOT EXISTS customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255),
    address VARCHAR(500),
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert sample customers
INSERT INTO customers (email, name, address, phone) VALUES 
('admin@example.com', 'Admin User', 'Jl. Merdeka No. 1, Jakarta', '08123456789'),
('user@example.com', 'Regular User', 'Jl. Sudirman No. 10, Bandung', '08198765432')
ON DUPLICATE KEY UPDATE email=email;

-- Use order_db
USE order_db;

-- Create orders table
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    total_price DECIMAL(19,2) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create order_items table
CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(19,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- Use inventory_db
USE inventory_db;

-- Create inventory_reservations table
CREATE TABLE IF NOT EXISTS inventory_reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    order_id BIGINT,
    status VARCHAR(50) DEFAULT 'RESERVED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Use shipping_db
USE shipping_db;

-- Create shipments table
CREATE TABLE IF NOT EXISTS shipments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    tracking_number VARCHAR(100),
    status VARCHAR(50) DEFAULT 'PENDING',
    estimated_delivery DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
