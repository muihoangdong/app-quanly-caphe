<?php
// Kết nối MySQL (service "mysql" trong cùng docker-compose, cổng nội bộ 3306).
// Tài khoản "cafe" chỉ có quyền SELECT / INSERT / UPDATE / DELETE trên CSDL quanlycafe.
return [
 'dsn' => 'mysql:host=mysql;port=3306;dbname=quanlycafe;charset=utf8mb4',
 'user' => 'cafe',
 'password' => 'cafe123',
 // Khóa ký token đăng nhập (đổi chuỗi này khi triển khai thật)
 'bi_mat' => 'quanlycafe-doi-chuoi-nay-khi-trien-khai',
 // Token hết hạn sau 30 ngày
 'han_token' => 30 * 24 * 3600,
];
