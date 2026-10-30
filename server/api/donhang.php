<?php
// Đơn gọi món / hóa đơn: tạo đơn, thêm / sửa món, hủy, thanh toán, tra cứu hóa đơn.
require __DIR__ . '/chung.php';
require __DIR__ . '/don_chung.php';

const MA_PHUONG_THUC = ['CASH', 'BANK_TRANSFER', 'BANK_QR', 'CARD', 'E_WALLET'];

/** Tạo đơn mới (banId = 0: mang đi / tại quầy). */
function taoOrder(PDO $pdo, int $banId, int $soKhach, ?string $tenKhach, int $nhanVienId): int
{
 if ($banId <= 0) {
  return them($pdo, 'INSERT INTO Orders (table_id, user_id, guest_count, note) VALUES (NULL, ?, ?, ?)',
   [$nhanVienId, $soKhach, rongThanhNull($tenKhach)]);
 }
 if (dong($pdo, 'SELECT status FROM `Tables` WHERE id = ? FOR UPDATE', [$banId]) === null) {
  throw new LoiApi('Không tìm thấy bàn', 404);
 }
 if (so($pdo, 'SELECT COUNT(*) FROM Orders WHERE table_id = ? AND status = \'OPEN\'', [$banId]) > 0) {
  throw new LoiApi('Bàn đã có khách');
 }
 $hoaDonId = them($pdo, 'INSERT INTO Orders (table_id, user_id, guest_count) VALUES (?, ?, ?)',
  [$banId, $nhanVienId, $soKhach]);
 ghi($pdo, 'UPDATE `Tables` SET status = \'SERVING\' WHERE id = ?', [$banId]);
 return $hoaDonId;
}

/** Kiểm tra món trong đơn còn mở. */
function kiemTraDongMon(PDO $pdo, int $chiTietId): void
{
 $hoaDonId = so($pdo, 'SELECT order_id FROM OrderDetails WHERE id = ?', [$chiTietId]);
 if ($hoaDonId === 0) throw new LoiApi('Không tìm thấy món trong đơn', 404);
 khoaDonDangMo($pdo, $hoaDonId);
}

chay(function (PDO $pdo, array $yc, array $cfg) {
 $toi = nguoiDung($pdo, $cfg);
 $nhanVienId = (int)$toi['id'];

 switch ($yc['action'] ?? '') {
  case 'taoDon':
   $dsMon = is_array($yc['dsMon'] ?? null) ? $yc['dsMon'] : [];
   if (count($dsMon) === 0) throw new LoiApi('Giỏ hàng đang trống');
   thanhCong(giaoDich($pdo, function () use ($pdo, $yc, $dsMon, $nhanVienId) {
    $hoaDonId = taoOrder($pdo, nguyen($yc, 'banId'), max(1, nguyen($yc, 'soKhach')),
     chuoi($yc, 'tenKhach'), $nhanVienId);
    themMon($pdo, $hoaDonId, $dsMon);
    return $hoaDonId;
   }));

  case 'layDon':
   thanhCong(layHoaDon($pdo, nguyen($yc, 'id')));

  case 'dangPhucVu':
   thanhCong(cacDong($pdo, SQL_HOA_DON . ' WHERE o.status = \'OPEN\' ORDER BY o.created_at DESC'));

  case 'timDaThanhToan':
   $sql = SQL_HOA_DON . ' WHERE o.status = \'PAID\'';
   $p = [];
   $tuKhoa = str_replace('#', '', chuoi($yc, 'tuKhoa'));
   if ($tuKhoa !== '') {
    $sql .= ' AND (i.invoice_code LIKE ? OR t.name LIKE ? OR o.note LIKE ?)';
    array_push($p, "%$tuKhoa%", "%$tuKhoa%", "%$tuKhoa%");
   }
   $denNgay = (int)($yc['denNgay'] ?? 0);
   if ($denNgay > 0) {
    $sql .= ' AND i.paid_at >= FROM_UNIXTIME(?) AND i.paid_at < FROM_UNIXTIME(?)';
    array_push($p, intdiv((int)($yc['tuNgay'] ?? 0), 1000), intdiv($denNgay, 1000));
   }
   $sql .= ' ORDER BY i.paid_at DESC, i.id DESC';
   $gioiHan = nguyen($yc, 'gioiHan');
   if ($gioiHan > 0) $sql .= ' LIMIT ' . $gioiHan;
   thanhCong(cacDong($pdo, $sql, $p));

  case 'chiTiet':
   thanhCong(cacDong($pdo, 'SELECT od.id, od.order_id AS hoaDonId, od.drink_id AS thucUongId,'
     . ' od.drink_name AS tenMon, od.unit_price AS donGia, od.quantity AS soLuong, od.note AS ghiChu,'
     . ' od.status AS trangThai, UNIX_TIMESTAMP(od.created_at) * 1000 AS thoiGian, c.name AS danhMuc,'
     . ' d.image_uri AS uriAnh'
     . ' FROM OrderDetails od LEFT JOIN Drinks d ON d.id = od.drink_id'
     . ' LEFT JOIN Categories c ON c.id = d.category_id'
     . ' WHERE od.order_id = ? ORDER BY od.status = \'SERVED\', od.created_at, od.id', [nguyen($yc, 'hoaDonId')]));

  case 'themMon':
   $hoaDonId = nguyen($yc, 'hoaDonId');
   $dsMon = is_array($yc['dsMon'] ?? null) ? $yc['dsMon'] : [];
   giaoDich($pdo, function () use ($pdo, $hoaDonId, $dsMon) {
    khoaDonDangMo($pdo, $hoaDonId);
    themMon($pdo, $hoaDonId, $dsMon);
   });
   thanhCong();

  case 'capNhatSoLuong':
   $chiTietId = nguyen($yc, 'chiTietId');
   $soLuong = nguyen($yc, 'soLuong');
   giaoDich($pdo, function () use ($pdo, $chiTietId, $soLuong) {
    kiemTraDongMon($pdo, $chiTietId);
    if ($soLuong <= 0) {
     ghi($pdo, 'DELETE FROM OrderDetails WHERE id = ?', [$chiTietId]);
    } else {
     ghi($pdo, 'UPDATE OrderDetails SET quantity = ? WHERE id = ?', [$soLuong, $chiTietId]);
    }
   });
   thanhCong();

  case 'capNhatTrangThaiMon':
   $chiTietId = nguyen($yc, 'chiTietId');
   $daLen = !empty($yc['daLenMon']);
   giaoDich($pdo, function () use ($pdo, $chiTietId, $daLen) {
    kiemTraDongMon($pdo, $chiTietId);
    ghi($pdo, 'UPDATE OrderDetails SET status = ? WHERE id = ?', [$daLen ? 'SERVED' : 'PREPARING', $chiTietId]);
   });
   thanhCong();

  case 'yeuCauThanhToan':
   $hoaDonId = nguyen($yc, 'hoaDonId');
   giaoDich($pdo, function () use ($pdo, $hoaDonId) {
    ghi($pdo, 'UPDATE `Tables` SET status = \'WAITING_PAYMENT\' WHERE id = ?',
     [khoaDonDangMo($pdo, $hoaDonId)['table_id']]);
   });
   thanhCong();

  case 'huy':
   $hoaDonId = nguyen($yc, 'hoaDonId');
   giaoDich($pdo, function () use ($pdo, $hoaDonId) {
    $banId = khoaDonDangMo($pdo, $hoaDonId)['table_id'];
    ghi($pdo, 'UPDATE Orders SET status = \'CANCELLED\', closed_at = NOW() WHERE id = ?', [$hoaDonId]);
    ghi($pdo, 'UPDATE `Tables` SET status = \'EMPTY\' WHERE id = ?', [$banId]);
   });
   thanhCong();

  case 'thanhToan':
   $hoaDonId = nguyen($yc, 'hoaDonId');
   $phuongThuc = chuoi($yc, 'phuongThuc');
   if (!in_array($phuongThuc, MA_PHUONG_THUC, true)) throw new LoiApi('Phương thức thanh toán không hợp lệ', 400);
   $giamGia = (int)($yc['giamGia'] ?? 0);
   $khachDua = (int)($yc['khachDua'] ?? 0);
   thanhCong(giaoDich($pdo, function () use ($pdo, $hoaDonId, $phuongThuc, $giamGia, $khachDua, $nhanVienId) {
    $banId = khoaDonDangMo($pdo, $hoaDonId)['table_id'];
    $tamTinh = so($pdo, 'SELECT COALESCE(SUM(quantity * unit_price), 0) FROM OrderDetails WHERE order_id = ?',
     [$hoaDonId]);
    if ($tamTinh <= 0) throw new LoiApi('Đơn chưa có món để thanh toán');
    $giam = max(0, min($giamGia, $tamTinh));
    $tong = lamTron($tamTinh - $giam);
    $nhan = $phuongThuc === 'CASH' ? $khachDua : $tong;
    if ($nhan < $tong) throw new LoiApi('Khách đưa chưa đủ tiền');
    $hoaDonMoi = them($pdo, 'INSERT INTO Invoices (order_id, user_id, subtotal, discount, total,'
      . ' payment_method, amount_received, change_amount) VALUES (?, ?, ?, ?, ?, ?, ?, ?)',
     [$hoaDonId, $nhanVienId, $tamTinh, $giam, $tong, $phuongThuc, $nhan, $nhan - $tong]);
    ghi($pdo, 'UPDATE Invoices SET invoice_code = CONCAT(\'HD\', LPAD(id, 4, \'0\')) WHERE id = ?', [$hoaDonMoi]);
    ghi($pdo, 'UPDATE Orders SET status = \'PAID\', closed_at = NOW() WHERE id = ?', [$hoaDonId]);
    ghi($pdo, 'UPDATE OrderDetails SET status = \'SERVED\' WHERE order_id = ?', [$hoaDonId]);
    if ($banId !== null) ghi($pdo, 'UPDATE `Tables` SET status = \'EMPTY\' WHERE id = ?', [$banId]);
    return layHoaDon($pdo, $hoaDonId);
   }));
 }
});
