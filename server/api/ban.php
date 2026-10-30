<?php
// Bàn: sơ đồ bàn kèm đơn đang mở; quản lý thêm / sửa / xóa; đặt trước, chuyển bàn, gộp bàn.
require __DIR__ . '/chung.php';
require __DIR__ . '/don_chung.php';

const SQL_BAN = 'SELECT t.id, t.name AS tenBan, t.area AS khuVuc, t.seats AS soCho, t.status AS trangThai,'
 . ' o.id AS hoaDonId, o.guest_count AS soKhach, UNIX_TIMESTAMP(o.created_at) * 1000 AS gioVao,'
 . ' (SELECT COALESCE(SUM(quantity), 0) FROM OrderDetails WHERE order_id = o.id) AS soMon,'
 . ' (SELECT COALESCE(SUM(quantity * unit_price), 0) FROM OrderDetails WHERE order_id = o.id) AS tamTinh'
 . ' FROM `Tables` t LEFT JOIN Orders o ON o.table_id = t.id AND o.status = \'OPEN\'';

const MA_TRANG_THAI = ['EMPTY', 'SERVING', 'WAITING_PAYMENT', 'RESERVED', 'MAINTENANCE'];

function layBan(PDO $pdo, int $id): array
{
 $ban = dong($pdo, SQL_BAN . ' WHERE t.id = ?', [$id]);
 if ($ban === null) throw new LoiApi('Không tìm thấy bàn', 404);
 return $ban;
}

chay(function (PDO $pdo, array $yc, array $cfg) {
 $toi = nguoiDung($pdo, $cfg);

 switch ($yc['action'] ?? '') {
  case 'danhSach':
   thanhCong(cacDong($pdo, SQL_BAN . ' ORDER BY t.id'));

  case 'capNhatTrangThai':
   // Chỉ đổi giữa Trống / Đặt trước cho bàn chưa có đơn
   $id = nguyen($yc, 'id');
   $ma = chuoi($yc, 'trangThai');
   if (!in_array($ma, MA_TRANG_THAI, true)) throw new LoiApi('Trạng thái bàn không hợp lệ', 400);
   $ban = layBan($pdo, $id);
   $coKhach = in_array($ma, ['SERVING', 'WAITING_PAYMENT'], true);
   if ($ban['hoaDonId'] !== null && !$coKhach) {
    throw new LoiApi('Bàn đang có khách, hãy thanh toán hoặc hủy order trước');
   }
   if ($ban['hoaDonId'] === null && $coKhach) throw new LoiApi('Bàn chưa có order, hãy mở bàn để gọi món');
   ghi($pdo, 'UPDATE `Tables` SET status = ? WHERE id = ?', [$ma, $id]);
   thanhCong();

  case 'doiBan':
   $hoaDonId = nguyen($yc, 'hoaDonId');
   $banMoiId = nguyen($yc, 'banMoiId');
   giaoDich($pdo, function () use ($pdo, $hoaDonId, $banMoiId) {
    $banCu = khoaDonDangMo($pdo, $hoaDonId)['table_id'];
    $moi = layBan($pdo, $banMoiId);
    if ($moi['hoaDonId'] !== null || $moi['trangThai'] === 'RESERVED') throw new LoiApi('Bàn mới không trống');
    $cu = layBan($pdo, (int)$banCu);
    ghi($pdo, 'UPDATE Orders SET table_id = ? WHERE id = ?', [$banMoiId, $hoaDonId]);
    ghi($pdo, 'UPDATE `Tables` SET status = ? WHERE id = ?', [$cu['trangThai'], $banMoiId]);
    ghi($pdo, 'UPDATE `Tables` SET status = \'EMPTY\' WHERE id = ?', [$banCu]);
   });
   thanhCong();

  case 'gopBan':
   $nguonId = nguyen($yc, 'hoaDonNguonId');
   $dichId = nguyen($yc, 'hoaDonDichId');
   if ($nguonId === $dichId) throw new LoiApi('Không thể gộp vào chính nó');
   giaoDich($pdo, function () use ($pdo, $nguonId, $dichId) {
    $nguon = khoaDonDangMo($pdo, $nguonId);
    khoaDonDangMo($pdo, $dichId);
    ghi($pdo, 'UPDATE OrderDetails SET order_id = ? WHERE order_id = ?', [$dichId, $nguonId]);
    ghi($pdo, 'UPDATE Orders SET guest_count = LEAST(guest_count + ?, 255) WHERE id = ?',
     [$nguon['guest_count'], $dichId]);
    ghi($pdo, 'DELETE FROM Orders WHERE id = ?', [$nguonId]);
    ghi($pdo, 'UPDATE `Tables` SET status = \'EMPTY\' WHERE id = ?', [$nguon['table_id']]);
   });
   thanhCong();
 }

 // Thêm / sửa / xóa bàn: chỉ quản lý
 canQuanLy($toi);
 switch ($yc['action'] ?? '') {
  case 'them':
   them($pdo, 'INSERT INTO `Tables` (name, area, seats) VALUES (?, ?, ?)',
    [chuoi($yc, 'tenBan'), chuoi($yc, 'khuVuc'), nguyen($yc, 'soCho')]);
   thanhCong();

  case 'sua':
   ghi($pdo, 'UPDATE `Tables` SET name = ?, area = ?, seats = ? WHERE id = ?',
    [chuoi($yc, 'tenBan'), chuoi($yc, 'khuVuc'), nguyen($yc, 'soCho'), nguyen($yc, 'id')]);
   thanhCong();

  case 'xoa':
   $id = nguyen($yc, 'id');
   if (so($pdo, 'SELECT COUNT(*) FROM Orders WHERE table_id = ? AND status = \'OPEN\'', [$id]) > 0) {
    throw new LoiApi('Bàn đang có khách, không thể xóa');
   }
   if (so($pdo, 'SELECT COUNT(*) FROM Orders WHERE table_id = ?', [$id]) > 0) {
    throw new LoiApi('Bàn đã có lịch sử hóa đơn nên không xóa được. Hãy chuyển bàn sang "Bảo trì".');
   }
   ghi($pdo, 'DELETE FROM `Tables` WHERE id = ?', [$id]);
   thanhCong();
 }
});
