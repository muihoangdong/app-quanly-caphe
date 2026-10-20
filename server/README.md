# CSDL quản lý quán cà phê (MySQL trong Docker)

```
App Android (nhân viên / admin)  --JDBC (SQL)-->  MySQL 8.4 trong Docker (cổng 3307)
                                                        ^
                                          DBeaver xem / sửa dữ liệu
```

App kết nối thẳng tới MySQL bằng thư viện `mysql-connector-java 5.1.49`, không qua server trung gian.
Code SQL nằm ở `app/src/main/java/hutech/cntt/app_quanlycafe/data/CafeDatabase.java`.

## 1. Chạy hằng ngày

Mở Android Studio → bật máy ảo → bấm **Run**. Gradle tự chạy:

- `docker compose up -d` (bật MySQL; cần Docker Desktop đang mở)
- `adb reverse tcp:3307 tcp:3307` (máy ảo nối `127.0.0.1:3307` là tới MySQL trên máy tính)

Nên bật Docker Desktop → Settings → General → *Start Docker Desktop when you sign in*.

Tự chạy bằng tay (trong thư mục `server`):

```bash
docker compose up -d
```

- Lần đầu chạy, MySQL tự tạo CSDL `quanlycafe` và 7 bảng từ `mysql/init/01_schema.sql`.
  CSDL ban đầu **trống**: thêm bàn, danh mục, thức uống ngay trong app (hoặc bằng DBeaver).
- Dữ liệu nằm trong Docker volume `quanlycafe_mysql_data` (nhóm container `quanlycafe`) nên tắt / bật lại vẫn còn.
- Dừng: `docker compose down`. Xóa sạch dữ liệu: `docker compose down -v`
  hoặc chạy lại `mysql/dbeaver_tao_csdl.sql`.

## 2. Xem / sửa CSDL bằng DBeaver

New Database Connection → **MySQL**:

| Mục | Giá trị |
|---|---|
| Host | `localhost` |
| Port | `3307` (không dùng 3306 để khỏi trùng MySQL khác trên máy) |
| Database | `quanlycafe` |
| Username / Password | `root` / `root123` (toàn quyền, sửa cả cấu trúc bảng) |
| Hoặc | `cafe` / `cafe123` (tài khoản app dùng, chỉ đọc / ghi dữ liệu) |

Tab **Driver properties**: đặt `allowPublicKeyRetrieval = true` và `useSSL = false`
(nếu DBeaver báo lỗi "Public Key Retrieval is not allowed").

Tên bảng `Tables` trùng từ khóa MySQL nên khi tự viết SQL phải để trong dấu huyền: ``SELECT * FROM `Tables`;``

## 3. Các bảng

| Bảng | Chức năng |
|---|---|
| `Users` | Tài khoản (để dành cho chức năng đăng nhập sau này) |
| `Tables` | Bàn, khu vực, số chỗ, trạng thái: `EMPTY`, `SERVING`, `WAITING_PAYMENT`, `RESERVED`, `MAINTENANCE` |
| `Categories` | Danh mục thức uống |
| `Drinks` | Thức uống, giá bán, giá vốn, còn bán / tạm hết |
| `Orders` | Đơn gọi món theo bàn: `OPEN` → `PAID` hoặc `CANCELLED` |
| `OrderDetails` | Món và số lượng trong đơn (lưu lại tên + giá lúc gọi) |
| `Invoices` | Hóa đơn: tạm tính, giảm giá, VAT 8%, tổng (làm tròn nghìn), phương thức, tiền khách đưa |

## 4. Địa chỉ MySQL trong app (nút ⚙)

| Thiết bị | Địa chỉ |
|---|---|
| Máy ảo / điện thoại cắm USB (đã adb reverse) | `127.0.0.1:3307` (mặc định) |
| Điện thoại cùng Wi-Fi | `<IP máy tính>:3307` (xem IP bằng `ipconfig`, mở cổng 3307 trên Windows Firewall) |

## 5. Ghi chú kỹ thuật

- Driver JDBC 5.1 chỉ hỗ trợ kiểu đăng nhập `mysql_native_password`, nên `docker-compose.yml` bật
  `--mysql-native-password=ON` và tài khoản `cafe` được tạo với kiểu này.
- Các thao tác nhiều bước (mở bàn, thêm món, đổi / gộp bàn, thanh toán) chạy trong **transaction**:
  lỗi ở bất kỳ bước nào thì hoàn tác toàn bộ.
- Mật khẩu tài khoản `cafe` nằm trong app. Chấp nhận được vì đồ án chạy nội bộ; nếu triển khai thật
  nên thêm lớp API ở giữa để giấu mật khẩu và kiểm tra nghiệp vụ.
