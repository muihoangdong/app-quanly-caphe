-- =====================================================================
--  Tạo lại CSDL quanlycafe (MySQL 8) - dán vào SQL Editor của DBeaver
--  rồi bấm Alt + X (Execute SQL Script).
--  CẢNH BÁO: XÓA TOÀN BỘ dữ liệu cũ của 7 bảng bên dưới.
--  Kết nối bằng tài khoản root (localhost:3307, root / root123).
-- =====================================================================

CREATE DATABASE IF NOT EXISTS quanlycafe
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE quanlycafe;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS Invoices;
DROP TABLE IF EXISTS OrderDetails;
DROP TABLE IF EXISTS Orders;
DROP TABLE IF EXISTS Drinks;
DROP TABLE IF EXISTS Categories;
DROP TABLE IF EXISTS `Tables`;
DROP TABLE IF EXISTS Users;
SET FOREIGN_KEY_CHECKS = 1;

-- Tài khoản (để dành cho chức năng đăng nhập về sau)
CREATE TABLE Users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    role          ENUM('ADMIN', 'STAFF') NOT NULL DEFAULT 'STAFF',
    is_active     TINYINT(1)   NOT NULL DEFAULT 1,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB;

-- Bàn và trạng thái bàn (TABLES là từ khóa MySQL nên luôn viết `Tables`)
CREATE TABLE `Tables` (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(50) NOT NULL UNIQUE,
    area       VARCHAR(50) NOT NULL,
    seats      TINYINT UNSIGNED NOT NULL DEFAULT 4,
    status     ENUM('EMPTY', 'SERVING', 'WAITING_PAYMENT', 'RESERVED', 'MAINTENANCE')
               NOT NULL DEFAULT 'EMPTY',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB;

-- Danh mục thức uống
CREATE TABLE Categories (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB;

-- Thức uống và giá
CREATE TABLE Drinks (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    category_id    INT NOT NULL,
    code           VARCHAR(30),
    name           VARCHAR(150) NOT NULL,
    description    VARCHAR(500),
    price          INT UNSIGNED NOT NULL,
    cost_price     INT UNSIGNED NOT NULL DEFAULT 0,
    image_uri      VARCHAR(500),
    is_available   TINYINT(1) NOT NULL DEFAULT 1,
    is_best_seller TINYINT(1) NOT NULL DEFAULT 0,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_drinks_category FOREIGN KEY (category_id) REFERENCES Categories (id)
        ON DELETE RESTRICT
) ENGINE = InnoDB;

-- Đơn gọi món theo bàn
CREATE TABLE Orders (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    table_id    INT NOT NULL,
    user_id     INT NULL,
    guest_count TINYINT UNSIGNED NOT NULL DEFAULT 1,
    status      ENUM('OPEN', 'PAID', 'CANCELLED') NOT NULL DEFAULT 'OPEN',
    note        VARCHAR(255),
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at   DATETIME NULL,
    CONSTRAINT fk_orders_table FOREIGN KEY (table_id) REFERENCES `Tables` (id) ON DELETE RESTRICT,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES Users (id) ON DELETE SET NULL,
    INDEX idx_orders_status (status),
    INDEX idx_orders_table_status (table_id, status)
) ENGINE = InnoDB;

-- Chi tiết món và số lượng (lưu lại tên + giá lúc gọi món)
CREATE TABLE OrderDetails (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    order_id   INT NOT NULL,
    drink_id   INT NULL,
    drink_name VARCHAR(150) NOT NULL,
    unit_price INT UNSIGNED NOT NULL,
    quantity   SMALLINT UNSIGNED NOT NULL,
    note       VARCHAR(255) NOT NULL DEFAULT '',
    status     ENUM('PREPARING', 'SERVED') NOT NULL DEFAULT 'PREPARING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_details_order FOREIGN KEY (order_id) REFERENCES Orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_details_drink FOREIGN KEY (drink_id) REFERENCES Drinks (id) ON DELETE SET NULL,
    CONSTRAINT chk_details_quantity CHECK (quantity > 0)
) ENGINE = InnoDB;

-- Hóa đơn và thanh toán (mỗi đơn tối đa một hóa đơn)
CREATE TABLE Invoices (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    invoice_code    VARCHAR(30) UNIQUE,
    order_id        INT NOT NULL UNIQUE,
    user_id         INT NULL,
    subtotal        INT UNSIGNED NOT NULL,
    discount        INT UNSIGNED NOT NULL DEFAULT 0,
    vat             INT UNSIGNED NOT NULL DEFAULT 0,
    total           INT UNSIGNED NOT NULL,
    payment_method  ENUM('CASH', 'BANK_QR', 'CARD', 'E_WALLET') NOT NULL,
    amount_received INT UNSIGNED NOT NULL,
    change_amount   INT UNSIGNED NOT NULL DEFAULT 0,
    paid_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoices_order FOREIGN KEY (order_id) REFERENCES Orders (id) ON DELETE RESTRICT,
    CONSTRAINT fk_invoices_user FOREIGN KEY (user_id) REFERENCES Users (id) ON DELETE SET NULL,
    INDEX idx_invoices_paid_at (paid_at)
) ENGINE = InnoDB;

-- Tài khoản app Android dùng để kết nối (JDBC cần kiểu đăng nhập mysql_native_password)
CREATE USER IF NOT EXISTS 'cafe'@'%' IDENTIFIED WITH mysql_native_password BY 'cafe123';
ALTER USER 'cafe'@'%' IDENTIFIED WITH mysql_native_password BY 'cafe123';
GRANT SELECT, INSERT, UPDATE, DELETE ON quanlycafe.* TO 'cafe'@'%';
FLUSH PRIVILEGES;
