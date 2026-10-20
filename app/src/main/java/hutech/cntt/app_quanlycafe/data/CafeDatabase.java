package hutech.cntt.app_quanlycafe.data;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;

import hutech.cntt.app_quanlycafe.data.KetNoiMySQL.LoiCSDL;
import hutech.cntt.app_quanlycafe.model.Ban;
import hutech.cntt.app_quanlycafe.model.ChiTietHoaDon;
import hutech.cntt.app_quanlycafe.model.DanhMuc;
import hutech.cntt.app_quanlycafe.model.HoaDon;
import hutech.cntt.app_quanlycafe.model.ThucUong;

/**
 * Đọc / ghi CSDL MySQL quanlycafe bằng JDBC: bàn, danh mục, thức uống, đơn gọi món, hóa đơn.
 * Mỗi hàm mở một kết nối riêng và đóng ngay khi xong. Các hàm chạy đồng bộ nên luôn gọi qua {@link ChayNen}.
 * Thời gian trong CSDL là DATETIME, đổi sang mili-giây bằng UNIX_TIMESTAMP(...) * 1000.
 */
public class CafeDatabase {

    public static final double THUE_VAT = 0.08;

    private static CafeDatabase instance;
    private final KetNoiMySQL ketNoi;

    public static synchronized CafeDatabase getInstance(Context context) {
        if (instance == null) instance = new CafeDatabase(context);
        return instance;
    }

    private CafeDatabase(Context context) {
        ketNoi = new KetNoiMySQL(context);
    }

    public KetNoiMySQL getKetNoi() {
        return ketNoi;
    }

    // ===================== Tiện ích JDBC =====================

    private interface CongViec<T> {
        T lam(Connection c) throws SQLException, LoiCSDL;
    }

    /** Mở kết nối, chạy công việc rồi đóng kết nối. */
    private <T> T chay(CongViec<T> congViec) throws LoiCSDL {
        try (Connection c = ketNoi.moKetNoi()) {
            return congViec.lam(c);
        } catch (SQLException e) {
            throw KetNoiMySQL.dichLoi(e);
        }
    }

    /** Như chay() nhưng trong một transaction: lỗi ở bất kỳ câu lệnh nào thì hoàn tác tất cả. */
    private <T> T giaoDich(CongViec<T> congViec) throws LoiCSDL {
        try (Connection c = ketNoi.moKetNoi()) {
            c.setAutoCommit(false);
            try {
                T kq = congViec.lam(c);
                c.commit();
                return kq;
            } catch (SQLException | LoiCSDL | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw KetNoiMySQL.dichLoi(e);
        }
    }

    private static PreparedStatement lenh(Connection c, String sql, Object... thamSo) throws SQLException {
        PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        for (int i = 0; i < thamSo.length; i++) ps.setObject(i + 1, thamSo[i]);
        return ps;
    }

    /** INSERT / UPDATE / DELETE, trả về số dòng bị ảnh hưởng. */
    private static int ghi(Connection c, String sql, Object... thamSo) throws SQLException {
        try (PreparedStatement ps = lenh(c, sql, thamSo)) {
            return ps.executeUpdate();
        }
    }

    /** INSERT, trả về id tự tăng vừa tạo. */
    private static int them(Connection c, String sql, Object... thamSo) throws SQLException {
        try (PreparedStatement ps = lenh(c, sql, thamSo)) {
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** SELECT trả về một số (COUNT, SUM...). */
    private static long so(Connection c, String sql, Object... thamSo) throws SQLException {
        try (PreparedStatement ps = lenh(c, sql, thamSo); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    private interface DocDong<T> {
        T doc(ResultSet rs) throws SQLException;
    }

    private static <T> ArrayList<T> danhSach(Connection c, String sql, DocDong<T> docDong, Object... thamSo)
            throws SQLException {
        ArrayList<T> ds = new ArrayList<>();
        try (PreparedStatement ps = lenh(c, sql, thamSo); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) ds.add(docDong.doc(rs));
        }
        return ds;
    }

    // ===================== TRANG CHỦ =====================

    public void kiemTraKetNoi() throws LoiCSDL {
        chay(c -> so(c, "SELECT 1"));
    }

    /** Số bàn theo trạng thái + doanh thu trong khoảng thời gian (mili-giây). */
    public JSONObject layTongQuan(long tuNgay, long denNgay) throws LoiCSDL {
        return chay(c -> {
            try {
                JSONObject theoTrangThai = new JSONObject();
                for (int i = 0; i <= Ban.BAO_TRI; i++) theoTrangThai.put(Ban.maTrangThai(i), 0);
                int tongBan = 0;
                try (PreparedStatement ps = lenh(c, "SELECT status, COUNT(*) FROM `Tables` GROUP BY status");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        theoTrangThai.put(rs.getString(1), rs.getInt(2));
                        tongBan += rs.getInt(2);
                    }
                }
                JSONObject kq = new JSONObject();
                kq.put("total_tables", tongBan);
                kq.put("tables_by_status", theoTrangThai);
                try (PreparedStatement ps = lenh(c, "SELECT COUNT(*), COALESCE(SUM(total), 0),"
                                + " COALESCE(SUM(CASE WHEN payment_method = 'CASH' THEN total ELSE 0 END), 0)"
                                + " FROM Invoices WHERE paid_at >= FROM_UNIXTIME(?) AND paid_at < FROM_UNIXTIME(?)",
                        tuNgay / 1000, denNgay / 1000);
                     ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    kq.put("invoice_count", rs.getInt(1));
                    kq.put("revenue", rs.getLong(2));
                    kq.put("cash_revenue", rs.getLong(3));
                }
                kq.put("open_orders", so(c, "SELECT COUNT(*) FROM Orders WHERE status = 'OPEN'"));
                kq.put("open_amount", so(c, "SELECT COALESCE(SUM(d.quantity * d.unit_price), 0)"
                        + " FROM OrderDetails d JOIN Orders o ON o.id = d.order_id WHERE o.status = 'OPEN'"));
                return kq;
            } catch (JSONException e) {
                throw new LoiCSDL("Lỗi đọc dữ liệu tổng quan");
            }
        });
    }

    // ===================== BÀN =====================

    private static final String SQL_BAN = "SELECT t.id, t.name, t.area, t.seats, t.status,"
            + " o.id, o.guest_count, UNIX_TIMESTAMP(o.created_at) * 1000,"
            + " (SELECT COALESCE(SUM(quantity), 0) FROM OrderDetails WHERE order_id = o.id),"
            + " (SELECT COALESCE(SUM(quantity * unit_price), 0) FROM OrderDetails WHERE order_id = o.id)"
            + " FROM `Tables` t LEFT JOIN Orders o ON o.table_id = t.id AND o.status = 'OPEN'";

    private static Ban docBan(ResultSet rs) throws SQLException {
        Ban ban = new Ban(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4), Ban.tuMa(rs.getString(5)));
        int hoaDonId = rs.getInt(6);
        if (!rs.wasNull()) {
            ban.setHoaDonId(hoaDonId);
            ban.setSoKhach(rs.getInt(7));
            ban.setGioVao(rs.getLong(8));
            ban.setSoMon(rs.getInt(9));
            ban.setTamTinh(rs.getLong(10));
        }
        return ban;
    }

    /** khuVuc = null để lấy tất cả. */
    public ArrayList<Ban> layDanhSachBan(String khuVuc) throws LoiCSDL {
        return chay(c -> khuVuc == null
                ? danhSach(c, SQL_BAN + " ORDER BY t.id", CafeDatabase::docBan)
                : danhSach(c, SQL_BAN + " WHERE t.area = ? ORDER BY t.id", CafeDatabase::docBan, khuVuc));
    }

    private Ban layBan(Connection c, int banId) throws SQLException, LoiCSDL {
        ArrayList<Ban> ds = danhSach(c, SQL_BAN + " WHERE t.id = ?", CafeDatabase::docBan, banId);
        if (ds.isEmpty()) throw new LoiCSDL("Không tìm thấy bàn");
        return ds.get(0);
    }

    /** Khu vực và số bàn trong khu vực. */
    public LinkedHashMap<String, Integer> layDanhSachKhuVuc() throws LoiCSDL {
        return chay(c -> {
            LinkedHashMap<String, Integer> kq = new LinkedHashMap<>();
            try (PreparedStatement ps = lenh(c, "SELECT area, COUNT(*) FROM `Tables` GROUP BY area ORDER BY area");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) kq.put(rs.getString(1), rs.getInt(2));
            }
            return kq;
        });
    }

    public void themBan(Ban ban) throws LoiCSDL {
        chay(c -> them(c, "INSERT INTO `Tables` (name, area, seats) VALUES (?, ?, ?)",
                ban.getTenBan(), ban.getKhuVuc(), ban.getSoCho()));
    }

    public void suaBan(Ban ban) throws LoiCSDL {
        chay(c -> ghi(c, "UPDATE `Tables` SET name = ?, area = ?, seats = ? WHERE id = ?",
                ban.getTenBan(), ban.getKhuVuc(), ban.getSoCho(), ban.getId()));
    }

    public void xoaBan(int banId) throws LoiCSDL {
        chay(c -> {
            if (so(c, "SELECT COUNT(*) FROM Orders WHERE table_id = ? AND status = 'OPEN'", banId) > 0) {
                throw new LoiCSDL("Bàn đang có khách, không thể xóa");
            }
            if (so(c, "SELECT COUNT(*) FROM Orders WHERE table_id = ?", banId) > 0) {
                throw new LoiCSDL("Bàn đã có lịch sử hóa đơn nên không xóa được. Hãy chuyển bàn sang \"Bảo trì\".");
            }
            return ghi(c, "DELETE FROM `Tables` WHERE id = ?", banId);
        });
    }

    /** Cập nhật trạng thái bàn bằng tay: chỉ đổi Trống / Đã đặt / Bảo trì khi bàn không có khách. */
    public void capNhatTrangThaiBan(int banId, int trangThai) throws LoiCSDL {
        chay(c -> {
            Ban ban = layBan(c, banId);
            boolean trangThaiCoKhach = trangThai == Ban.CO_KHACH || trangThai == Ban.CHO_THANH_TOAN;
            if (!ban.isTrong() && !trangThaiCoKhach) {
                throw new LoiCSDL("Bàn đang có khách, hãy thanh toán hoặc hủy order trước");
            }
            if (ban.isTrong() && trangThaiCoKhach) {
                throw new LoiCSDL("Bàn chưa có order, hãy mở bàn để gọi món");
            }
            return ghi(c, "UPDATE `Tables` SET status = ? WHERE id = ?", Ban.maTrangThai(trangThai), banId);
        });
    }

    public void doiTenKhuVuc(String tenCu, String tenMoi) throws LoiCSDL {
        chay(c -> ghi(c, "UPDATE `Tables` SET area = ? WHERE area = ?", tenMoi, tenCu));
    }

    /** Mở bàn: tạo đơn gọi món mới, trả về id đơn. */
    public int moBan(int banId, int soKhach) throws LoiCSDL {
        return giaoDich(c -> {
            String trangThai;
            try (PreparedStatement ps = lenh(c, "SELECT status FROM `Tables` WHERE id = ? FOR UPDATE", banId);
                 ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new LoiCSDL("Không tìm thấy bàn");
                trangThai = rs.getString(1);
            }
            if ("MAINTENANCE".equals(trangThai)) throw new LoiCSDL("Bàn đang bảo trì");
            if (so(c, "SELECT COUNT(*) FROM Orders WHERE table_id = ? AND status = 'OPEN'", banId) > 0) {
                throw new LoiCSDL("Bàn đã có khách");
            }
            int hoaDonId = them(c, "INSERT INTO Orders (table_id, guest_count) VALUES (?, ?)", banId, soKhach);
            ghi(c, "UPDATE `Tables` SET status = 'SERVING' WHERE id = ?", banId);
            return hoaDonId;
        });
    }

    // ===================== DANH MỤC =====================

    public ArrayList<DanhMuc> layDanhSachDanhMuc() throws LoiCSDL {
        return chay(c -> danhSach(c, "SELECT c.id, c.name,"
                        + " (SELECT COUNT(*) FROM Drinks d WHERE d.category_id = c.id) FROM Categories c ORDER BY c.id",
                rs -> new DanhMuc(rs.getInt(1), rs.getString(2), rs.getInt(3))));
    }

    public DanhMuc themDanhMuc(String ten) throws LoiCSDL {
        return chay(c -> new DanhMuc(them(c, "INSERT INTO Categories (name) VALUES (?)", ten), ten, 0));
    }

    public void doiTenDanhMuc(int id, String tenMoi) throws LoiCSDL {
        chay(c -> ghi(c, "UPDATE Categories SET name = ? WHERE id = ?", tenMoi, id));
    }

    public void xoaDanhMuc(int id) throws LoiCSDL {
        chay(c -> {
            long soMon = so(c, "SELECT COUNT(*) FROM Drinks WHERE category_id = ?", id);
            if (soMon > 0) {
                throw new LoiCSDL("Danh mục còn " + soMon + " món. Hãy chuyển hoặc xóa các món trước.");
            }
            return ghi(c, "DELETE FROM Categories WHERE id = ?", id);
        });
    }

    // ===================== THỨC UỐNG =====================

    private static final String SQL_THUC_UONG = "SELECT d.id, d.code, d.name, c.name, d.description, d.price,"
            + " d.cost_price, d.image_uri, d.is_available, d.is_best_seller, d.category_id"
            + " FROM Drinks d JOIN Categories c ON c.id = d.category_id";

    private static ThucUong docThucUong(ResultSet rs) throws SQLException {
        ThucUong tu = new ThucUong(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                rs.getString(5), rs.getLong(6), rs.getLong(7), rs.getString(8),
                rs.getInt(9) == 1, rs.getInt(10) == 1);
        tu.setDanhMucId(rs.getInt(11));
        return tu;
    }

    /** danhMucId = 0 để lấy mọi danh mục. */
    public ArrayList<ThucUong> layDanhSachThucUong(int danhMucId, String tuKhoa, boolean chiMonConBan)
            throws LoiCSDL {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        ArrayList<Object> thamSo = new ArrayList<>();
        if (danhMucId > 0) {
            where.append(" AND d.category_id = ?");
            thamSo.add(danhMucId);
        }
        if (tuKhoa != null && !tuKhoa.trim().isEmpty()) {
            where.append(" AND (d.name LIKE ? OR d.code LIKE ?)");
            thamSo.add("%" + tuKhoa.trim() + "%");
            thamSo.add("%" + tuKhoa.trim() + "%");
        }
        if (chiMonConBan) where.append(" AND d.is_available = 1");
        String sql = SQL_THUC_UONG + where + " ORDER BY d.is_best_seller DESC, c.id, d.name";
        return chay(c -> danhSach(c, sql, CafeDatabase::docThucUong, thamSo.toArray()));
    }

    public ThucUong layThucUong(int id) throws LoiCSDL {
        return chay(c -> {
            ArrayList<ThucUong> ds = danhSach(c, SQL_THUC_UONG + " WHERE d.id = ?", CafeDatabase::docThucUong, id);
            if (ds.isEmpty()) throw new LoiCSDL("Không tìm thấy món");
            return ds.get(0);
        });
    }

    private static Object[] giaTriThucUong(ThucUong tu) {
        return new Object[]{tu.getDanhMucId(), rongThanhNull(tu.getMaMon()), tu.getTenMon(),
                rongThanhNull(tu.getMoTa()), tu.getGia(), tu.getGiaVon(), rongThanhNull(tu.getUriAnh()),
                tu.isConBan() ? 1 : 0, tu.isBanChay() ? 1 : 0};
    }

    private static String rongThanhNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    public void themThucUong(ThucUong tu) throws LoiCSDL {
        chay(c -> them(c, "INSERT INTO Drinks (category_id, code, name, description, price, cost_price,"
                + " image_uri, is_available, is_best_seller) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", giaTriThucUong(tu)));
    }

    public void suaThucUong(ThucUong tu) throws LoiCSDL {
        Object[] giaTri = giaTriThucUong(tu);
        Object[] thamSo = new Object[giaTri.length + 1];
        System.arraycopy(giaTri, 0, thamSo, 0, giaTri.length);
        thamSo[giaTri.length] = tu.getId();
        chay(c -> ghi(c, "UPDATE Drinks SET category_id = ?, code = ?, name = ?, description = ?, price = ?,"
                + " cost_price = ?, image_uri = ?, is_available = ?, is_best_seller = ? WHERE id = ?", thamSo));
    }

    /** OrderDetails giữ lại tên + giá món nên hóa đơn cũ không bị ảnh hưởng. */
    public void xoaThucUong(int id) throws LoiCSDL {
        chay(c -> ghi(c, "DELETE FROM Drinks WHERE id = ?", id));
    }

    // ===================== ĐƠN GỌI MÓN / HÓA ĐƠN =====================

    private static final String SQL_HOA_DON = "SELECT o.id, o.table_id, t.name, t.area, o.guest_count, o.status,"
            + " UNIX_TIMESTAMP(o.created_at) * 1000, UNIX_TIMESTAMP(o.closed_at) * 1000,"
            + " (SELECT COALESCE(SUM(quantity), 0) FROM OrderDetails WHERE order_id = o.id),"
            + " (SELECT COALESCE(SUM(quantity * unit_price), 0) FROM OrderDetails WHERE order_id = o.id),"
            + " i.id, i.invoice_code, i.subtotal, i.discount, i.vat, i.total, i.payment_method,"
            + " i.amount_received, UNIX_TIMESTAMP(i.paid_at) * 1000"
            + " FROM Orders o JOIN `Tables` t ON t.id = o.table_id LEFT JOIN Invoices i ON i.order_id = o.id";

    private static HoaDon docHoaDon(ResultSet rs) throws SQLException {
        HoaDon hd = new HoaDon();
        hd.setId(rs.getInt(1));
        hd.setBanId(rs.getInt(2));
        hd.setTenBan(rs.getString(3));
        hd.setKhuVuc(rs.getString(4));
        hd.setSoKhach(rs.getInt(5));
        String trangThai = rs.getString(6);
        hd.setTrangThai("PAID".equals(trangThai) ? HoaDon.DA_THANH_TOAN
                : "CANCELLED".equals(trangThai) ? HoaDon.DA_HUY : HoaDon.DANG_PHUC_VU);
        hd.setGioVao(rs.getLong(7));
        hd.setSoMon(rs.getInt(9));
        rs.getInt(11);
        if (rs.wasNull()) {
            // Chưa có hóa đơn: đang phục vụ hoặc đã hủy
            hd.setMaHoaDon(String.format(Locale.ROOT, "ORD-%04d", hd.getId()));
            hd.setTamTinh(rs.getLong(10));
            hd.setGioRa(rs.getLong(8));
        } else {
            hd.setMaHoaDon(rs.getString(12));
            hd.setTamTinh(rs.getLong(13));
            hd.setGiamGia(rs.getLong(14));
            hd.setVat(rs.getLong(15));
            hd.setTongTien(rs.getLong(16));
            hd.setPhuongThuc(HoaDon.tenPhuongThuc(rs.getString(17)));
            hd.setKhachDua(rs.getLong(18));
            hd.setGioRa(rs.getLong(19));
        }
        return hd;
    }

    private HoaDon layHoaDon(Connection c, int id) throws SQLException, LoiCSDL {
        ArrayList<HoaDon> ds = danhSach(c, SQL_HOA_DON + " WHERE o.id = ?", CafeDatabase::docHoaDon, id);
        if (ds.isEmpty()) throw new LoiCSDL("Không tìm thấy đơn");
        return ds.get(0);
    }

    public HoaDon layHoaDon(int id) throws LoiCSDL {
        return chay(c -> layHoaDon(c, id));
    }

    /** trangThai = -1 để lấy tất cả; denNgay = 0 để không lọc thời gian (lọc theo giờ thanh toán / đóng / mở). */
    public ArrayList<HoaDon> layDanhSachHoaDon(int trangThai, long tuNgay, long denNgay) throws LoiCSDL {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        ArrayList<Object> thamSo = new ArrayList<>();
        if (trangThai >= 0) {
            where.append(" AND o.status = ?");
            thamSo.add(trangThai == HoaDon.DA_THANH_TOAN ? "PAID"
                    : trangThai == HoaDon.DA_HUY ? "CANCELLED" : "OPEN");
        }
        if (denNgay > 0) {
            where.append(" AND COALESCE(i.paid_at, o.closed_at, o.created_at) >= FROM_UNIXTIME(?)"
                    + " AND COALESCE(i.paid_at, o.closed_at, o.created_at) < FROM_UNIXTIME(?)");
            thamSo.add(tuNgay / 1000);
            thamSo.add(denNgay / 1000);
        }
        String sql = SQL_HOA_DON + where
                + " ORDER BY o.status = 'OPEN' DESC, COALESCE(i.paid_at, o.closed_at, o.created_at) DESC";
        return chay(c -> danhSach(c, sql, CafeDatabase::docHoaDon, thamSo.toArray()));
    }

    public ArrayList<HoaDon> layHoaDonDaThanhToan(long tuNgay, long denNgay) throws LoiCSDL {
        return layDanhSachHoaDon(HoaDon.DA_THANH_TOAN, tuNgay, denNgay);
    }

    public ArrayList<ChiTietHoaDon> layChiTietHoaDon(int hoaDonId) throws LoiCSDL {
        return chay(c -> danhSach(c, "SELECT od.id, od.order_id, od.drink_id, od.drink_name, od.unit_price,"
                + " od.quantity, od.note, od.status, UNIX_TIMESTAMP(od.created_at) * 1000, c.name, d.image_uri"
                + " FROM OrderDetails od LEFT JOIN Drinks d ON d.id = od.drink_id"
                + " LEFT JOIN Categories c ON c.id = d.category_id"
                + " WHERE od.order_id = ? ORDER BY od.status = 'SERVED', od.created_at, od.id", rs -> {
            ChiTietHoaDon ct = new ChiTietHoaDon();
            ct.setId(rs.getInt(1));
            ct.setHoaDonId(rs.getInt(2));
            ct.setThucUongId(rs.getInt(3));
            ct.setTenMon(rs.getString(4));
            ct.setDonGia(rs.getLong(5));
            ct.setSoLuong(rs.getInt(6));
            ct.setGhiChu(rs.getString(7));
            ct.setTrangThai("SERVED".equals(rs.getString(8)) ? ChiTietHoaDon.DA_LEN_MON : ChiTietHoaDon.DANG_PHA_CHE);
            ct.setThoiGian(rs.getLong(9));
            ct.setDanhMuc(rs.getString(10));
            ct.setUriAnh(rs.getString(11));
            return ct;
        }, hoaDonId));
    }

    /** Khóa dòng đơn để cập nhật an toàn trong transaction; báo lỗi nếu đơn đã đóng. Trả về {id bàn, số khách}. */
    private static int[] khoaDonDangMo(Connection c, int hoaDonId) throws SQLException, LoiCSDL {
        try (PreparedStatement ps = lenh(c, "SELECT status, table_id, guest_count FROM Orders WHERE id = ? FOR UPDATE",
                hoaDonId); ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new LoiCSDL("Không tìm thấy đơn");
            if (!"OPEN".equals(rs.getString(1))) throw new LoiCSDL("Đơn đã thanh toán hoặc đã hủy");
            return new int[]{rs.getInt(2), rs.getInt(3)};
        }
    }

    /** Đơn chứa dòng chi tiết này phải còn đang mở. */
    private static void kiemTraDongMon(Connection c, int chiTietId) throws SQLException, LoiCSDL {
        long hoaDonId = so(c, "SELECT order_id FROM OrderDetails WHERE id = ?", chiTietId);
        if (hoaDonId == 0) throw new LoiCSDL("Không tìm thấy món trong đơn");
        khoaDonDangMo(c, (int) hoaDonId);
    }

    /** Gửi các món trong giỏ xuống quầy Bar; gộp với dòng đang pha chế cùng món + ghi chú + giá. */
    public void themMonVaoHoaDon(int hoaDonId, ArrayList<ChiTietHoaDon> dsMon) throws LoiCSDL {
        giaoDich(c -> {
            khoaDonDangMo(c, hoaDonId);
            for (ChiTietHoaDon ct : dsMon) {
                String ten;
                long gia;
                try (PreparedStatement ps = lenh(c, "SELECT name, price, is_available FROM Drinks WHERE id = ?",
                        ct.getThucUongId()); ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new LoiCSDL("Món không còn trong thực đơn");
                    ten = rs.getString(1);
                    gia = rs.getLong(2);
                    if (rs.getInt(3) != 1) throw new LoiCSDL(ten + " đang tạm hết");
                }
                String ghiChu = ct.getGhiChu() == null ? "" : ct.getGhiChu();
                long dongCu = so(c, "SELECT id FROM OrderDetails WHERE order_id = ? AND drink_id = ? AND note = ?"
                        + " AND status = 'PREPARING' AND unit_price = ?", hoaDonId, ct.getThucUongId(), ghiChu, gia);
                if (dongCu > 0) {
                    ghi(c, "UPDATE OrderDetails SET quantity = quantity + ? WHERE id = ?", ct.getSoLuong(), dongCu);
                } else {
                    them(c, "INSERT INTO OrderDetails (order_id, drink_id, drink_name, unit_price, quantity, note)"
                            + " VALUES (?, ?, ?, ?, ?, ?)", hoaDonId, ct.getThucUongId(), ten, gia, ct.getSoLuong(), ghiChu);
                }
            }
            return null;
        });
    }

    /** soLuong = 0 thì bỏ món. */
    public void capNhatSoLuong(int chiTietId, int soLuong) throws LoiCSDL {
        giaoDich(c -> {
            kiemTraDongMon(c, chiTietId);
            return soLuong <= 0
                    ? ghi(c, "DELETE FROM OrderDetails WHERE id = ?", chiTietId)
                    : ghi(c, "UPDATE OrderDetails SET quantity = ? WHERE id = ?", soLuong, chiTietId);
        });
    }

    public void capNhatTrangThaiMon(int chiTietId, int trangThai) throws LoiCSDL {
        giaoDich(c -> {
            kiemTraDongMon(c, chiTietId);
            return ghi(c, "UPDATE OrderDetails SET status = ? WHERE id = ?",
                    trangThai == ChiTietHoaDon.DA_LEN_MON ? "SERVED" : "PREPARING", chiTietId);
        });
    }

    public void xoaChiTiet(int chiTietId) throws LoiCSDL {
        capNhatSoLuong(chiTietId, 0);
    }

    /** In tạm tính: bàn chuyển sang "Chờ thanh toán". */
    public void yeuCauThanhToan(int hoaDonId) throws LoiCSDL {
        giaoDich(c -> ghi(c, "UPDATE `Tables` SET status = 'WAITING_PAYMENT' WHERE id = ?",
                khoaDonDangMo(c, hoaDonId)[0]));
    }

    public void huyHoaDon(int hoaDonId) throws LoiCSDL {
        giaoDich(c -> {
            int banId = khoaDonDangMo(c, hoaDonId)[0];
            ghi(c, "UPDATE Orders SET status = 'CANCELLED', closed_at = NOW() WHERE id = ?", hoaDonId);
            return ghi(c, "UPDATE `Tables` SET status = 'EMPTY' WHERE id = ?", banId);
        });
    }

    /** Đổi bàn: chuyển đơn sang bàn trống khác, bàn cũ thành trống. */
    public void doiBan(int hoaDonId, int banMoiId) throws LoiCSDL {
        giaoDich(c -> {
            int banCu = khoaDonDangMo(c, hoaDonId)[0];
            Ban moi = layBan(c, banMoiId);
            if (!moi.isTrong() || moi.getTrangThai() == Ban.BAO_TRI) throw new LoiCSDL("Bàn mới không trống");
            Ban cu = layBan(c, banCu);
            ghi(c, "UPDATE Orders SET table_id = ? WHERE id = ?", banMoiId, hoaDonId);
            ghi(c, "UPDATE `Tables` SET status = ? WHERE id = ?", Ban.maTrangThai(cu.getTrangThai()), banMoiId);
            return ghi(c, "UPDATE `Tables` SET status = 'EMPTY' WHERE id = ?", banCu);
        });
    }

    /** Gộp bàn: chuyển toàn bộ món của đơn nguồn sang đơn đích, bàn nguồn thành trống. */
    public void gopBan(int hoaDonNguonId, int hoaDonDichId) throws LoiCSDL {
        if (hoaDonNguonId == hoaDonDichId) throw new LoiCSDL("Không thể gộp vào chính nó");
        giaoDich(c -> {
            int[] nguon = khoaDonDangMo(c, hoaDonNguonId);
            khoaDonDangMo(c, hoaDonDichId);
            ghi(c, "UPDATE OrderDetails SET order_id = ? WHERE order_id = ?", hoaDonDichId, hoaDonNguonId);
            ghi(c, "UPDATE Orders SET guest_count = LEAST(guest_count + ?, 255) WHERE id = ?", nguon[1], hoaDonDichId);
            ghi(c, "DELETE FROM Orders WHERE id = ?", hoaDonNguonId);
            return ghi(c, "UPDATE `Tables` SET status = 'EMPTY' WHERE id = ?", nguon[0]);
        });
    }

    /**
     * Tạo hóa đơn & xác nhận thanh toán trong một transaction:
     * tạm tính lấy lại từ OrderDetails -> trừ giảm giá -> cộng VAT 8% -> làm tròn tới nghìn đồng.
     * Sau đó đơn chuyển "Đã thanh toán", bàn trở về trống.
     */
    public HoaDon thanhToan(int hoaDonId, long giamGia, String phuongThuc, long khachDua) throws LoiCSDL {
        String maPhuongThuc = HoaDon.maPhuongThuc(phuongThuc);
        return giaoDich(c -> {
            int banId = khoaDonDangMo(c, hoaDonId)[0];
            long tamTinh = so(c, "SELECT COALESCE(SUM(quantity * unit_price), 0) FROM OrderDetails WHERE order_id = ?",
                    hoaDonId);
            if (tamTinh <= 0) throw new LoiCSDL("Đơn chưa có món để thanh toán");
            long giam = Math.max(0, Math.min(giamGia, tamTinh));
            long vat = Math.round((tamTinh - giam) * THUE_VAT);
            long tong = TienIch.lamTron(tamTinh - giam + vat);
            long nhan = "CASH".equals(maPhuongThuc) ? khachDua : tong;
            if (nhan < tong) throw new LoiCSDL("Khách đưa chưa đủ tiền");

            int hoaDonMoi = them(c, "INSERT INTO Invoices (order_id, subtotal, discount, vat, total, payment_method,"
                            + " amount_received, change_amount) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    hoaDonId, tamTinh, giam, vat, tong, maPhuongThuc, nhan, nhan - tong);
            ghi(c, "UPDATE Invoices SET invoice_code = CONCAT('HD', DATE_FORMAT(paid_at, '%Y%m%d'), '-',"
                    + " LPAD(id, 4, '0')) WHERE id = ?", hoaDonMoi);
            ghi(c, "UPDATE Orders SET status = 'PAID', closed_at = NOW() WHERE id = ?", hoaDonId);
            ghi(c, "UPDATE OrderDetails SET status = 'SERVED' WHERE order_id = ?", hoaDonId);
            ghi(c, "UPDATE `Tables` SET status = 'EMPTY' WHERE id = ?", banId);
            return layHoaDon(c, hoaDonId);
        });
    }

    // ===================== BÁO CÁO =====================

    /** Mỗi phần tử: {tên món, số lượng, doanh thu}. */
    public ArrayList<Object[]> layMonBanChay(long tuNgay, long denNgay, int gioiHan) throws LoiCSDL {
        return chay(c -> danhSach(c, "SELECT od.drink_name, SUM(od.quantity) AS sl, SUM(od.quantity * od.unit_price) AS dt"
                        + " FROM OrderDetails od JOIN Invoices i ON i.order_id = od.order_id"
                        + " WHERE i.paid_at >= FROM_UNIXTIME(?) AND i.paid_at < FROM_UNIXTIME(?)"
                        + " GROUP BY od.drink_name ORDER BY sl DESC, dt DESC LIMIT ?",
                rs -> new Object[]{rs.getString(1), rs.getInt(2), rs.getLong(3)},
                tuNgay / 1000, denNgay / 1000, gioiHan));
    }
}
