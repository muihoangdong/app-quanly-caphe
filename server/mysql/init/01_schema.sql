-- =====================================================================
--  CSDL quản lý quán cà phê (MySQL 8)
--  Chạy tự động một lần khi container MySQL khởi tạo volume lần đầu.
--  Không có dữ liệu mẫu: thêm bàn, danh mục, thức uống từ app hoặc DBeaver.
-- =====================================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS quanlycafe
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE quanlycafe;

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

-- Bàn và trạng thái bàn
-- (TABLES là từ khóa của MySQL nên tên bảng luôn được đặt trong dấu `Tables`)
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

-- Đơn gọi món theo bàn (mỗi lượt khách ngồi bàn là một đơn)
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

-- Chi tiết món và số lượng
-- drink_name / unit_price được lưu lại để hóa đơn cũ không đổi khi sửa giá hoặc xóa món
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

-- Hóa đơn và thanh toán (mỗi đơn có tối đa một hóa đơn)
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
