<?php
// Hàm dùng chung cho các API: trả JSON, kết nối PDO, xác thực token, giao dịch.
// Mọi API nhận POST JSON { "action": "...", ... } và trả { success, message, total, data }.
declare(strict_types=1);
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');

/** Lỗi nghiệp vụ: thông báo hiện thẳng cho người dùng. */
class LoiApi extends Exception
{
 public int $maHttp;

 public function __construct(string $thongBao, int $maHttp = 422)
 {
  parent::__construct($thongBao);
  $this->maHttp = $maHttp;
 }
}

function traVe(int $status, array $body): void
{
 http_response_code($status);
 echo json_encode($body, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
 exit;
}

function thanhCong($data = null, string $thongBao = 'Thành công'): void
{
 $total = is_array($data) && array_is_list($data) ? count($data) : 0;
 traVe(200, ['success' => true, 'message' => $thongBao, 'total' => $total, 'data' => $data]);
}

// ----- Truy vấn -----

function so(PDO $pdo, string $sql, array $p = []): int
{
 $st = $pdo->prepare($sql);
 $st->execute($p);
 return (int)($st->fetchColumn() ?: 0);
}

function ghi(PDO $pdo, string $sql, array $p = []): int
{
 $st = $pdo->prepare($sql);
 $st->execute($p);
 return $st->rowCount();
}

function them(PDO $pdo, string $sql, array $p = []): int
{
 $st = $pdo->prepare($sql);
 $st->execute($p);
 return (int)$pdo->lastInsertId();
}

function dong(PDO $pdo, string $sql, array $p = []): ?array
{
 $st = $pdo->prepare($sql);
 $st->execute($p);
 $kq = $st->fetch();
 return $kq === false ? null : $kq;
}

function cacDong(PDO $pdo, string $sql, array $p = []): array
{
 $st = $pdo->prepare($sql);
 $st->execute($p);
 return $st->fetchAll();
}

function giaoDich(PDO $pdo, callable $viec)
{
 $pdo->beginTransaction();
 try {
  $kq = $viec();
  $pdo->commit();
  return $kq;
 } catch (Throwable $e) {
  $pdo->rollBack();
  throw $e;
 }
}

// ----- Tham số -----

function chuoi(array $yc, string $khoa): string
{
 $v = $yc[$khoa] ?? '';
 return is_string($v) ? trim($v) : (is_scalar($v) ? trim((string)$v) : '');
}

function nguyen(array $yc, string $khoa): int
{
 return (int)($yc[$khoa] ?? 0);
}

function rongThanhNull(?string $s): ?string
{
 return $s === null || trim($s) === '' ? null : trim($s);
}

/** Làm tròn tới nghìn đồng (giống TienIch.lamTron bên app). */
function lamTron(int $soTien): int
{
 return (int)round($soTien / 1000) * 1000;
}

// ----- Đăng nhập: token ký HMAC "id.hetHan.chuKy", app gửi lại qua header X-Token -----

function taoToken(int $id, array $cfg): string
{
 $noiDung = $id . '.' . (time() + $cfg['han_token']);
 return $noiDung . '.' . hash_hmac('sha256', $noiDung, $cfg['bi_mat']);
}

/** Người gọi API: kiểm tra token và tài khoản còn hoạt động. */
function nguoiDung(PDO $pdo, array $cfg): array
{
 // Header "X-Token" (Apache bỏ header Authorization trước khi tới PHP)
 $phan = explode('.', (string)($_SERVER['HTTP_X_TOKEN'] ?? ''));
 if (count($phan) !== 3
     || !hash_equals(hash_hmac('sha256', $phan[0] . '.' . $phan[1], $cfg['bi_mat']), $phan[2])
     || (int)$phan[1] < time()) {
  throw new LoiApi('Phiên đăng nhập đã hết hạn, hãy đăng nhập lại', 401);
 }
 $nv = dong($pdo, 'SELECT id, username, full_name, role, is_active FROM Users WHERE id = ?', [(int)$phan[0]]);
 if ($nv === null || (int)$nv['is_active'] !== 1) {
  throw new LoiApi('Tài khoản đã bị khóa, hãy đăng nhập lại', 401);
 }
 return $nv;
}

function canQuanLy(array $nv): void
{
 if ($nv['role'] !== 'ADMIN') throw new LoiApi('Chỉ quản lý mới được thực hiện thao tác này', 403);
}

/**
 * Chạy một API: đọc yêu cầu, mở kết nối, gọi $xuLy($pdo, $yeuCau, $cfg) và đổi lỗi thành JSON.
 * $xuLy kết thúc bằng thanhCong(...); không khớp action nào thì báo lỗi.
 */
function chay(callable $xuLy): void
{
 if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
  header('Allow: POST');
  traVe(405, ['success' => false, 'message' => 'Chỉ hỗ trợ POST']);
 }
 $yeuCau = json_decode(file_get_contents('php://input') ?: '{}', true);
 if (!is_array($yeuCau)) traVe(400, ['success' => false, 'message' => 'JSON không hợp lệ']);
 try {
  $cfg = require __DIR__ . '/config.php';
  $pdo = new PDO($cfg['dsn'], $cfg['user'], $cfg['password'], [
   PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
   PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
   PDO::ATTR_EMULATE_PREPARES => false,
  ]);
  $pdo->exec("SET time_zone = '+07:00'");
  $xuLy($pdo, $yeuCau, $cfg);
  throw new LoiApi('Thao tác không hợp lệ: ' . ($yeuCau['action'] ?? ''), 400);
 } catch (LoiApi $e) {
  traVe($e->maHttp, ['success' => false, 'message' => $e->getMessage()]);
 } catch (PDOException $e) {
  error_log($e->getMessage());
  $ma = $e->errorInfo[1] ?? null;
  $thongBao = match ($ma) {
   1062 => 'Tên đã tồn tại, hãy chọn tên khác',
   1451 => 'Dữ liệu đang được dùng ở nơi khác nên không xóa được',
   1452 => 'Dữ liệu liên kết không tồn tại',
   default => 'Lỗi cơ sở dữ liệu. Kiểm tra MySQL trong Docker đã chạy.',
  };
  traVe($ma === null ? 503 : 409, ['success' => false, 'message' => $thongBao]);
 } catch (Throwable $e) {
  error_log($e->getMessage());
  traVe(500, ['success' => false, 'message' => 'Lỗi máy chủ']);
 }
}
