<?php
// Phần dùng chung giữa ban.php và donhang.php: khóa đơn đang mở, thêm món, đọc đơn / hóa đơn.

/** Đơn mang đi (không gắn bàn): tên lấy từ note, không có thì "Mang đi #id". */
const SQL_HOA_DON = 'SELECT o.id, o.table_id AS banId,'
 . ' COALESCE(t.name, NULLIF(o.note, \'\'), CONCAT(\'Mang đi #\', o.id)) AS tenBan,'
 . ' COALESCE(t.area, \'Mang đi / tại quầy\') AS khuVuc, o.guest_count AS soKhach, o.status AS trangThai,'
 . ' UNIX_TIMESTAMP(o.created_at) * 1000 AS gioVao, UNIX_TIMESTAMP(o.closed_at) * 1000 AS gioDong,'
 . ' (SELECT COALESCE(SUM(quantity), 0) FROM OrderDetails WHERE order_id = o.id) AS soMon,'
 . ' (SELECT COALESCE(SUM(quantity * unit_price), 0) FROM OrderDetails WHERE order_id = o.id) AS tamTinhDon,'
 . ' i.id AS hoaDonId, i.invoice_code AS maHoaDon, i.subtotal AS tamTinh, i.discount AS giamGia,'
 . ' i.total AS tongTien, i.payment_method AS phuongThuc, i.amount_received AS khachDua,'
 . ' UNIX_TIMESTAMP(i.paid_at) * 1000 AS gioThanhToan, u.full_name AS tenNhanVien'
 . ' FROM Orders o LEFT JOIN `Tables` t ON t.id = o.table_id LEFT JOIN Invoices i ON i.order_id = o.id'
 . ' LEFT JOIN Users u ON u.id = COALESCE(i.user_id, o.user_id)';

function layHoaDon(PDO $pdo, int $id): array
{
 $hd = dong($pdo, SQL_HOA_DON . ' WHERE o.id = ?', [$id]);
 if ($hd === null) throw new LoiApi('Không tìm thấy đơn', 404);
 return $hd;
}

/** Khóa dòng đơn (FOR UPDATE) và kiểm tra đơn còn mở. Trả về table_id, guest_count. */
function khoaDonDangMo(PDO $pdo, int $hoaDonId): array
{
 $don = dong($pdo, 'SELECT status, table_id, guest_count FROM Orders WHERE id = ? FOR UPDATE', [$hoaDonId]);
 if ($don === null) throw new LoiApi('Không tìm thấy đơn', 404);
 if ($don['status'] !== 'OPEN') throw new LoiApi('Đơn đã thanh toán hoặc đã hủy');
 return $don;
}

/** Thêm món vào đơn; món trùng (cùng ghi chú, đang pha chế, cùng giá) thì cộng dồn số lượng. */
function themMon(PDO $pdo, int $hoaDonId, array $dsMon): void
{
 foreach ($dsMon as $ct) {
  $thucUongId = (int)($ct['thucUongId'] ?? 0);
  $soLuong = max(1, (int)($ct['soLuong'] ?? 1));
  $ghiChu = trim((string)($ct['ghiChu'] ?? ''));
  $mon = dong($pdo, 'SELECT name, price, is_available FROM Drinks WHERE id = ?', [$thucUongId]);
  if ($mon === null) throw new LoiApi('Món không còn trong thực đơn');
  if ((int)$mon['is_available'] !== 1) throw new LoiApi($mon['name'] . ' đang tạm hết');
  $dongCu = so($pdo, 'SELECT id FROM OrderDetails WHERE order_id = ? AND drink_id = ? AND note = ?'
   . ' AND status = \'PREPARING\' AND unit_price = ?', [$hoaDonId, $thucUongId, $ghiChu, $mon['price']]);
  if ($dongCu > 0) {
   ghi($pdo, 'UPDATE OrderDetails SET quantity = quantity + ? WHERE id = ?', [$soLuong, $dongCu]);
  } else {
   them($pdo, 'INSERT INTO OrderDetails (order_id, drink_id, drink_name, unit_price, quantity, note)'
    . ' VALUES (?, ?, ?, ?, ?, ?)', [$hoaDonId, $thucUongId, $mon['name'], $mon['price'], $soLuong, $ghiChu]);
  }
 }
}
