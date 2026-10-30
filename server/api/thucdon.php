<?php
// Thực đơn: danh mục và món. Xem thì ai cũng được, thêm / sửa / xóa chỉ quản lý.
require __DIR__ . '/chung.php';

const SQL_THUC_UONG = 'SELECT d.id, d.code AS maMon, d.name AS tenMon, c.name AS danhMuc, d.description AS moTa,'
 . ' d.price AS gia, d.cost_price AS giaVon, d.image_uri AS uriAnh, d.is_available AS conBan,'
 . ' d.is_best_seller AS banChay, d.category_id AS danhMucId'
 . ' FROM Drinks d JOIN Categories c ON c.id = d.category_id';

/** Giá trị cột theo thứ tự category_id, code, name, description, price, cost_price, image_uri, is_available, is_best_seller. */
function giaTriMon(array $m): array
{
 return [(int)($m['danhMucId'] ?? 0), rongThanhNull(chuoi($m, 'maMon')), chuoi($m, 'tenMon'),
  rongThanhNull(chuoi($m, 'moTa')), (int)($m['gia'] ?? 0), (int)($m['giaVon'] ?? 0),
  rongThanhNull(chuoi($m, 'uriAnh')), !empty($m['conBan']) ? 1 : 0, !empty($m['banChay']) ? 1 : 0];
}

chay(function (PDO $pdo, array $yc, array $cfg) {
 $toi = nguoiDung($pdo, $cfg);

 switch ($yc['action'] ?? '') {
  case 'danhMuc':
   thanhCong(cacDong($pdo, 'SELECT c.id, c.name AS ten,'
    . ' (SELECT COUNT(*) FROM Drinks d WHERE d.category_id = c.id) AS soMon FROM Categories c ORDER BY c.id'));

  case 'danhSachMon':
   // Gồm cả món tạm hết (Gọi món hiện làm mờ để nhân viên báo khách)
   $sql = SQL_THUC_UONG . ' WHERE 1=1';
   $p = [];
   $danhMucId = nguyen($yc, 'danhMucId');
   if ($danhMucId > 0) {
    $sql .= ' AND d.category_id = ?';
    $p[] = $danhMucId;
   }
   $tuKhoa = chuoi($yc, 'tuKhoa');
   if ($tuKhoa !== '') {
    $sql .= ' AND (d.name LIKE ? OR d.code LIKE ?)';
    array_push($p, "%$tuKhoa%", "%$tuKhoa%");
   }
   thanhCong(cacDong($pdo, $sql . ' ORDER BY d.is_best_seller DESC, c.id, d.name', $p));

  case 'layMon':
   $mon = dong($pdo, SQL_THUC_UONG . ' WHERE d.id = ?', [nguyen($yc, 'id')]);
   if ($mon === null) throw new LoiApi('Không tìm thấy món', 404);
   thanhCong($mon);
 }

 canQuanLy($toi);
 switch ($yc['action'] ?? '') {
  case 'themDanhMuc':
   thanhCong(them($pdo, 'INSERT INTO Categories (name) VALUES (?)', [chuoi($yc, 'ten')]));

  case 'doiTenDanhMuc':
   ghi($pdo, 'UPDATE Categories SET name = ? WHERE id = ?', [chuoi($yc, 'ten'), nguyen($yc, 'id')]);
   thanhCong();

  case 'xoaDanhMuc':
   $id = nguyen($yc, 'id');
   $soMon = so($pdo, 'SELECT COUNT(*) FROM Drinks WHERE category_id = ?', [$id]);
   if ($soMon > 0) throw new LoiApi("Danh mục còn $soMon món. Hãy chuyển hoặc xóa các món trước.");
   ghi($pdo, 'DELETE FROM Categories WHERE id = ?', [$id]);
   thanhCong();

  case 'themMon':
   them($pdo, 'INSERT INTO Drinks (category_id, code, name, description, price, cost_price,'
    . ' image_uri, is_available, is_best_seller) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)', giaTriMon($yc));
   thanhCong();

  case 'suaMon':
   ghi($pdo, 'UPDATE Drinks SET category_id = ?, code = ?, name = ?, description = ?, price = ?,'
    . ' cost_price = ?, image_uri = ?, is_available = ?, is_best_seller = ? WHERE id = ?',
    [...giaTriMon($yc), nguyen($yc, 'id')]);
   thanhCong();

  case 'xoaMon':
   ghi($pdo, 'DELETE FROM Drinks WHERE id = ?', [nguyen($yc, 'id')]);
   thanhCong();
 }
});
