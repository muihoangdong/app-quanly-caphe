<?php
// Cấu hình quán (bảng Settings) và kiểm tra kết nối.
require __DIR__ . '/chung.php';

chay(function (PDO $pdo, array $yc, array $cfg) {
 $action = $yc['action'] ?? '';

 // Dùng ở hộp thoại đổi địa chỉ máy chủ, chưa cần đăng nhập
 if ($action === 'kiemTra') {
  so($pdo, 'SELECT 1');
  thanhCong(null, 'Kết nối được MySQL');
 }

 nguoiDung($pdo, $cfg);
 if ($action === 'lay') {
  $kq = ['shop_name' => 'Espresso POS', 'bank_account' => ''];
  foreach (cacDong($pdo, 'SELECT setting_key, setting_value FROM Settings') as $d) {
   $kq[$d['setting_key']] = $d['setting_value'];
  }
  thanhCong($kq);
 }
});
