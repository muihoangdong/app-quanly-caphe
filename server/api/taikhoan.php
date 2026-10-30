<?php
// Tài khoản: đăng nhập, đổi mật khẩu; quản lý thêm / khóa / đặt lại mật khẩu nhân viên.
require __DIR__ . '/chung.php';

const MAT_KHAU_MAC_DINH = '123456';
const SQL_NHAN_VIEN = 'SELECT id, username AS tenDangNhap, full_name AS hoTen, role = \'ADMIN\' AS quanLy,'
 . ' is_active AS hoatDong FROM Users';

chay(function (PDO $pdo, array $yc, array $cfg) {
 $action = $yc['action'] ?? '';

 if ($action === 'dangNhap') {
  $nv = dong($pdo, SQL_NHAN_VIEN . ' WHERE username = ? AND password_hash = SHA2(?, 256)',
   [chuoi($yc, 'tenDangNhap'), (string)($yc['matKhau'] ?? '')]);
  if ($nv === null) throw new LoiApi('Sai tên đăng nhập hoặc mật khẩu');
  if ((int)$nv['hoatDong'] !== 1) throw new LoiApi('Tài khoản đã bị khóa, hãy liên hệ quản lý', 403);
  thanhCong(['nhanVien' => $nv, 'token' => taoToken((int)$nv['id'], $cfg)]);
 }

 $toi = nguoiDung($pdo, $cfg);

 switch ($action) {
  case 'nguoiDangNhap':
   thanhCong(dong($pdo, SQL_NHAN_VIEN . ' WHERE id = ?', [$toi['id']]));

  case 'doiMatKhau':
   $soDong = ghi($pdo, 'UPDATE Users SET password_hash = SHA2(?, 256) WHERE id = ? AND password_hash = SHA2(?, 256)',
    [(string)($yc['matKhauMoi'] ?? ''), $toi['id'], (string)($yc['matKhauCu'] ?? '')]);
   if ($soDong === 0) throw new LoiApi('Mật khẩu hiện tại không đúng');
   thanhCong();
 }

 // Các thao tác còn lại chỉ dành cho quản lý
 canQuanLy($toi);
 switch ($action) {
  case 'danhSach':
   thanhCong(cacDong($pdo, SQL_NHAN_VIEN . ' ORDER BY is_active DESC, role = \'ADMIN\' DESC, id'));

  case 'them':
   thanhCong(them($pdo, 'INSERT INTO Users (username, password_hash, full_name, role, phone)'
     . ' VALUES (?, SHA2(?, 256), ?, ?, ?)',
    [chuoi($yc, 'tenDangNhap'), MAT_KHAU_MAC_DINH, chuoi($yc, 'hoTen'),
     !empty($yc['quanLy']) ? 'ADMIN' : 'STAFF', rongThanhNull(chuoi($yc, 'soDienThoai'))]));

  case 'datLaiMatKhau':
   ghi($pdo, 'UPDATE Users SET password_hash = SHA2(?, 256) WHERE id = ?', [MAT_KHAU_MAC_DINH, nguyen($yc, 'id')]);
   thanhCong();

  case 'khoa':
   $id = nguyen($yc, 'id');
   $khoa = !empty($yc['khoa']);
   if ($khoa && $id === (int)$toi['id']) throw new LoiApi('Không thể tự khóa tài khoản đang đăng nhập');
   if ($khoa) {
    $conLai = so($pdo, 'SELECT COUNT(*) FROM Users WHERE is_active = 1 AND role = \'ADMIN\' AND id <> ?', [$id]);
    $laQuanLy = so($pdo, 'SELECT COUNT(*) FROM Users WHERE id = ? AND role = \'ADMIN\'', [$id]);
    if ($laQuanLy > 0 && $conLai === 0) throw new LoiApi('Phải còn ít nhất một quản lý hoạt động');
   }
   ghi($pdo, 'UPDATE Users SET is_active = ? WHERE id = ?', [$khoa ? 0 : 1, $id]);
   thanhCong();
 }
});
