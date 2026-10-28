package hutech.cntt.app_quanlycafe.data;

import android.content.Context;
import android.content.SharedPreferences;

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
import hutech.cntt.app_quanlycafe.model.NhanVien;
import hutech.cntt.app_quanlycafe.model.ThucUong;


public class CafeDatabase {

    private static final String TEN_FILE = "phien_lam_viec";
    private static final String KHOA_NHAN_VIEN = "nhan_vien_id";
    private static final String KHOA_QUAN_LY = "la_quan_ly";

    public static final String CD_TEN_QUAN = "shop_name";
    public static final String CD_TAI_KHOAN = "bank_account";

    private static CafeDatabase instance;
    private final KetNoiMySQL ketNoi;
    private final Context context;

    public static synchronized CafeDatabase getInstance(Context context) {
        if (instance == null) instance = new CafeDatabase(context);
        return instance;
    }

    private CafeDatabase(Context context) {
        this.context = context.getApplicationContext();
        ketNoi = new KetNoiMySQL(this.context);
    }

    public KetNoiMySQL getKetNoi() {
        return ketNoi;
    }



    private interface CongViec<T> {
        T lam(Connection c) throws SQLException, LoiCSDL;
    }


    private <T> T chay(CongViec<T> congViec) throws LoiCSDL {
        try (Connection c = ketNoi.moKetNoi()) {
            return congViec.lam(c);
        } catch (SQLException e) {
            throw KetNoiMySQL.dichLoi(e);
        }
    }


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


    private static int ghi(Connection c, String sql, Object... thamSo) throws SQLException {
        try (PreparedStatement ps = lenh(c, sql, thamSo)) {
            return ps.executeUpdate();
        }
    }


    private static int them(Connection c, String sql, Object... thamSo) throws SQLException {
        try (PreparedStatement ps = lenh(c, sql, thamSo)) {
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

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



    public void kiemTraKetNoi() throws LoiCSDL {
        chay(c -> so(c, "SELECT 1"));
    }


    //CẤU HÌNH QUÁN (Settings)

    public LinkedHashMap<String, String> layCauHinh() throws LoiCSDL {
        return chay(c -> {
            LinkedHashMap<String, String> kq = new LinkedHashMap<>();
            kq.put(CD_TEN_QUAN, "Espresso POS");
            kq.put(CD_TAI_KHOAN, "");
            try (PreparedStatement ps = lenh(c, "SELECT setting_key, setting_value FROM Settings");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) kq.put(rs.getString(1), rs.getString(2));
            }
            return kq;
        });
    }

    //NHÂN VIÊN (Users)

    public static final String MAT_KHAU_MAC_DINH = "123456";

    private static final String SQL_NHAN_VIEN =
            "SELECT id, username, full_name, role, is_active FROM Users";

    private static NhanVien docNhanVien(ResultSet rs) throws SQLException {
        return new NhanVien(rs.getInt(1), rs.getString(2), rs.getString(3),
                "ADMIN".equals(rs.getString(4)), rs.getInt(5) == 1);
    }


    private SharedPreferences phien() {
        return context.getSharedPreferences(TEN_FILE, Context.MODE_PRIVATE);
    }

    private void luuPhien(NhanVien nv) {
        phien().edit().putInt(KHOA_NHAN_VIEN, nv.getId()).putBoolean(KHOA_QUAN_LY, nv.isQuanLy()).apply();
    }

    public boolean daDangNhap() {
        return idNguoiDangNhap() > 0;
    }

    public int idNguoiDangNhap() {
        return phien().getInt(KHOA_NHAN_VIEN, 0);
    }

    public boolean laQuanLy() {
        return daDangNhap() && phien().getBoolean(KHOA_QUAN_LY, false);
    }

    private void canQuanLy() throws LoiCSDL {
        if (!laQuanLy()) throw new LoiCSDL("Chỉ quản lý mới được thực hiện thao tác này");
    }

    public NhanVien dangNhap(String tenDangNhap, String matKhau) throws LoiCSDL {
        ArrayList<NhanVien> ds = chay(c -> danhSach(c, SQL_NHAN_VIEN
                        + " WHERE username = ? AND password_hash = SHA2(?, 256)",
                CafeDatabase::docNhanVien, tenDangNhap.trim(), matKhau));
        if (ds.isEmpty()) throw new LoiCSDL("Sai tên đăng nhập hoặc mật khẩu");
        NhanVien nv = ds.get(0);
        if (!nv.isHoatDong()) throw new LoiCSDL("Tài khoản đã bị khóa, hãy liên hệ quản lý");
        luuPhien(nv);
        return nv;
    }

    public void dangXuat() {
        phien().edit().remove(KHOA_NHAN_VIEN).remove(KHOA_QUAN_LY).apply();
    }

    public NhanVien layNguoiDangNhap() throws LoiCSDL {
        int id = idNguoiDangNhap();
        if (id == 0) return null;
        ArrayList<NhanVien> ds = chay(c -> danhSach(c, SQL_NHAN_VIEN + " WHERE id = ? AND is_active = 1",
                CafeDatabase::docNhanVien, id));
        if (ds.isEmpty()) return null;
        luuPhien(ds.get(0));
        return ds.get(0);
    }

    private Integer nhanVien(Connection c) throws SQLException {
        long id = so(c, "SELECT id FROM Users WHERE id = ? AND is_active = 1", idNguoiDangNhap());
        return id == 0 ? null : (int) id;
    }

    public void doiMatKhau(String matKhauCu, String matKhauMoi) throws LoiCSDL {
        int soDong = chay(c -> ghi(c, "UPDATE Users SET password_hash = SHA2(?, 256)"
                + " WHERE id = ? AND password_hash = SHA2(?, 256)", matKhauMoi, idNguoiDangNhap(), matKhauCu));
        if (soDong == 0) throw new LoiCSDL("Mật khẩu hiện tại không đúng");
    }

    //Quản lý tài khoản (chỉ quản lý)

    public ArrayList<NhanVien> layTatCaNhanVien() throws LoiCSDL {
        canQuanLy();
        return chay(c -> danhSach(c, SQL_NHAN_VIEN + " ORDER BY is_active DESC, role = 'ADMIN' DESC, id",
                CafeDatabase::docNhanVien));
    }

    public int themNhanVien(String tenDangNhap, String hoTen, boolean quanLy, String soDienThoai) throws LoiCSDL {
        canQuanLy();
        return chay(c -> them(c, "INSERT INTO Users (username, password_hash, full_name, role, phone)"
                        + " VALUES (?, SHA2(?, 256), ?, ?, ?)",
                tenDangNhap.trim(), MAT_KHAU_MAC_DINH, hoTen.trim(), quanLy ? "ADMIN" : "STAFF",
                rongThanhNull(soDienThoai)));
    }

    public void datLaiMatKhau(int id) throws LoiCSDL {
        canQuanLy();
        chay(c -> ghi(c, "UPDATE Users SET password_hash = SHA2(?, 256) WHERE id = ?", MAT_KHAU_MAC_DINH, id));
    }

    public void khoaTaiKhoan(int id, boolean khoa) throws LoiCSDL {
        canQuanLy();
        if (khoa && id == idNguoiDangNhap()) throw new LoiCSDL("Không thể tự khóa tài khoản đang đăng nhập");
        chay(c -> {
            if (khoa) {
                long conLai = so(c, "SELECT COUNT(*) FROM Users WHERE is_active = 1 AND role = 'ADMIN' AND id <> ?", id);
                long laQuanLy = so(c, "SELECT COUNT(*) FROM Users WHERE id = ? AND role = 'ADMIN'", id);
                if (laQuanLy > 0 && conLai == 0) throw new LoiCSDL("Phải còn ít nhất một quản lý hoạt động");
            }
            return ghi(c, "UPDATE Users SET is_active = ? WHERE id = ?", khoa ? 0 : 1, id);
        });
    }

    //BÀN

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

    public ArrayList<Ban> layDanhSachBan() throws LoiCSDL {
        return chay(c -> danhSach(c, SQL_BAN + " ORDER BY t.id", CafeDatabase::docBan));
    }

    private Ban layBan(Connection c, int banId) throws SQLException, LoiCSDL {
        ArrayList<Ban> ds = danhSach(c, SQL_BAN + " WHERE t.id = ?", CafeDatabase::docBan, banId);
        if (ds.isEmpty()) throw new LoiCSDL("Không tìm thấy bàn");
        return ds.get(0);
    }

    public void themBan(Ban ban) throws LoiCSDL {
        canQuanLy();
        chay(c -> them(c, "INSERT INTO `Tables` (name, area, seats) VALUES (?, ?, ?)",
                ban.getTenBan(), ban.getKhuVuc(), ban.getSoCho()));
    }

    public void suaBan(Ban ban) throws LoiCSDL {
        canQuanLy();
        chay(c -> ghi(c, "UPDATE `Tables` SET name = ?, area = ?, seats = ? WHERE id = ?",
                ban.getTenBan(), ban.getKhuVuc(), ban.getSoCho(), ban.getId()));
    }

    public void xoaBan(int banId) throws LoiCSDL {
        canQuanLy();
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

    private int taoOrder(Connection c, int banId, int soKhach, String tenKhach) throws SQLException, LoiCSDL {
        Integer nhanVien = nhanVien(c);
        if (banId <= 0) {
            return them(c, "INSERT INTO Orders (table_id, user_id, guest_count, note) VALUES (NULL, ?, ?, ?)",
                    nhanVien, soKhach, rongThanhNull(tenKhach));
        }
        try (PreparedStatement ps = lenh(c, "SELECT status FROM `Tables` WHERE id = ? FOR UPDATE", banId);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new LoiCSDL("Không tìm thấy bàn");
        }
        if (so(c, "SELECT COUNT(*) FROM Orders WHERE table_id = ? AND status = 'OPEN'", banId) > 0) {
            throw new LoiCSDL("Bàn đã có khách");
        }
        int hoaDonId = them(c, "INSERT INTO Orders (table_id, user_id, guest_count) VALUES (?, ?, ?)",
                banId, nhanVien, soKhach);
        ghi(c, "UPDATE `Tables` SET status = 'SERVING' WHERE id = ?", banId);
        return hoaDonId;
    }

    public int taoDonHang(int banId, int soKhach, String tenKhach, ArrayList<ChiTietHoaDon> dsMon) throws LoiCSDL {
        if (dsMon.isEmpty()) throw new LoiCSDL("Giỏ hàng đang trống");
        return giaoDich(c -> {
            int hoaDonId = taoOrder(c, banId, soKhach, tenKhach);
            themMon(c, hoaDonId, dsMon);
            return hoaDonId;
        });
    }

    //DANH MỤC

    public ArrayList<DanhMuc> layDanhSachDanhMuc() throws LoiCSDL {
        return chay(c -> danhSach(c, "SELECT c.id, c.name,"
                        + " (SELECT COUNT(*) FROM Drinks d WHERE d.category_id = c.id) FROM Categories c ORDER BY c.id",
                rs -> new DanhMuc(rs.getInt(1), rs.getString(2), rs.getInt(3))));
    }

    public DanhMuc themDanhMuc(String ten) throws LoiCSDL {
        canQuanLy();
        return chay(c -> new DanhMuc(them(c, "INSERT INTO Categories (name) VALUES (?)", ten), ten, 0));
    }

    public void doiTenDanhMuc(int id, String tenMoi) throws LoiCSDL {
        canQuanLy();
        chay(c -> ghi(c, "UPDATE Categories SET name = ? WHERE id = ?", tenMoi, id));
    }

    public void xoaDanhMuc(int id) throws LoiCSDL {
        canQuanLy();
        chay(c -> {
            long soMon = so(c, "SELECT COUNT(*) FROM Drinks WHERE category_id = ?", id);
            if (soMon > 0) {
                throw new LoiCSDL("Danh mục còn " + soMon + " món. Hãy chuyển hoặc xóa các món trước.");
            }
            return ghi(c, "DELETE FROM Categories WHERE id = ?", id);
        });
    }

    //THỨC UỐNG

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

    /** Gồm cả món tạm hết (Gọi món hiện làm mờ để nhân viên báo khách). */
    public ArrayList<ThucUong> layDanhSachThucUong(int danhMucId, String tuKhoa) throws LoiCSDL {
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
        canQuanLy();
        chay(c -> them(c, "INSERT INTO Drinks (category_id, code, name, description, price, cost_price,"
                + " image_uri, is_available, is_best_seller) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", giaTriThucUong(tu)));
    }

    public void suaThucUong(ThucUong tu) throws LoiCSDL {
        canQuanLy();
        Object[] giaTri = giaTriThucUong(tu);
        Object[] thamSo = new Object[giaTri.length + 1];
        System.arraycopy(giaTri, 0, thamSo, 0, giaTri.length);
        thamSo[giaTri.length] = tu.getId();
        chay(c -> ghi(c, "UPDATE Drinks SET category_id = ?, code = ?, name = ?, description = ?, price = ?,"
                + " cost_price = ?, image_uri = ?, is_available = ?, is_best_seller = ? WHERE id = ?", thamSo));
    }

    public void xoaThucUong(int id) throws LoiCSDL {
        canQuanLy();
        chay(c -> ghi(c, "DELETE FROM Drinks WHERE id = ?", id));
    }

    //ĐƠN GỌI MÓN / HÓA ĐƠN

    /** Đơn mang đi (không gắn bàn): tên lấy từ note, không có thì "Mang đi #id". */
    private static final String SQL_HOA_DON = "SELECT o.id, o.table_id,"
            + " COALESCE(t.name, NULLIF(o.note, ''), CONCAT('Mang đi #', o.id)),"
            + " COALESCE(t.area, 'Mang đi / tại quầy'), o.guest_count, o.status,"
            + " UNIX_TIMESTAMP(o.created_at) * 1000, UNIX_TIMESTAMP(o.closed_at) * 1000,"
            + " (SELECT COALESCE(SUM(quantity), 0) FROM OrderDetails WHERE order_id = o.id),"
            + " (SELECT COALESCE(SUM(quantity * unit_price), 0) FROM OrderDetails WHERE order_id = o.id),"
            + " i.id, i.invoice_code, i.subtotal, i.discount, i.total, i.payment_method,"
            + " i.amount_received, UNIX_TIMESTAMP(i.paid_at) * 1000, u.full_name"
            + " FROM Orders o LEFT JOIN `Tables` t ON t.id = o.table_id LEFT JOIN Invoices i ON i.order_id = o.id"
            + " LEFT JOIN Users u ON u.id = COALESCE(i.user_id, o.user_id)";

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
        hd.setTenNhanVien(rs.getString(19));
        rs.getInt(11);
        if (rs.wasNull()) {
            // Chưa có hóa đơn: đang phục vụ hoặc đã hủy
            hd.setMaHoaDon(String.format(Locale.ROOT, "DH%04d", hd.getId()));
            hd.setTamTinh(rs.getLong(10));
            hd.setGioRa(rs.getLong(8));
        } else {
            hd.setMaHoaDon(rs.getString(12));
            hd.setTamTinh(rs.getLong(13));
            hd.setGiamGia(rs.getLong(14));
            hd.setTongTien(rs.getLong(15));
            hd.setPhuongThuc(HoaDon.tenPhuongThuc(rs.getString(16)));
            hd.setKhachDua(rs.getLong(17));
            hd.setGioRa(rs.getLong(18));
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

    /** Các đơn đang phục vụ (chưa thanh toán / chưa hủy), mới nhất lên đầu. */
    public ArrayList<HoaDon> layDonDangPhucVu() throws LoiCSDL {
        return chay(c -> danhSach(c, SQL_HOA_DON + " WHERE o.status = 'OPEN' ORDER BY o.created_at DESC",
                CafeDatabase::docHoaDon));
    }

    public ArrayList<HoaDon> timHoaDonDaThanhToan(String tuKhoa, long tuNgay, long denNgay, int gioiHan)
            throws LoiCSDL {
        StringBuilder sql = new StringBuilder(SQL_HOA_DON).append(" WHERE o.status = 'PAID'");
        ArrayList<Object> thamSo = new ArrayList<>();
        if (tuKhoa != null && !tuKhoa.trim().isEmpty()) {
            String k = "%" + tuKhoa.trim().replace("#", "") + "%";
            sql.append(" AND (i.invoice_code LIKE ? OR t.name LIKE ? OR o.note LIKE ?)");
            thamSo.add(k);
            thamSo.add(k);
            thamSo.add(k);
        }
        if (denNgay > 0) {
            sql.append(" AND i.paid_at >= FROM_UNIXTIME(?) AND i.paid_at < FROM_UNIXTIME(?)");
            thamSo.add(tuNgay / 1000);
            thamSo.add(denNgay / 1000);
        }
        sql.append(" ORDER BY i.paid_at DESC, i.id DESC");
        if (gioiHan > 0) sql.append(" LIMIT ").append(gioiHan);
        return chay(c -> danhSach(c, sql.toString(), CafeDatabase::docHoaDon, thamSo.toArray()));
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

    private static int[] khoaDonDangMo(Connection c, int hoaDonId) throws SQLException, LoiCSDL {
        try (PreparedStatement ps = lenh(c, "SELECT status, table_id, guest_count FROM Orders WHERE id = ? FOR UPDATE",
                hoaDonId); ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new LoiCSDL("Không tìm thấy đơn");
            if (!"OPEN".equals(rs.getString(1))) throw new LoiCSDL("Đơn đã thanh toán hoặc đã hủy");
            return new int[]{rs.getInt(2), rs.getInt(3)};
        }
    }

    private static void kiemTraDongMon(Connection c, int chiTietId) throws SQLException, LoiCSDL {
        long hoaDonId = so(c, "SELECT order_id FROM OrderDetails WHERE id = ?", chiTietId);
        if (hoaDonId == 0) throw new LoiCSDL("Không tìm thấy món trong đơn");
        khoaDonDangMo(c, (int) hoaDonId);
    }

    public void themMonVaoHoaDon(int hoaDonId, ArrayList<ChiTietHoaDon> dsMon) throws LoiCSDL {
        giaoDich(c -> {
            khoaDonDangMo(c, hoaDonId);
            themMon(c, hoaDonId, dsMon);
            return null;
        });
    }

    private static void themMon(Connection c, int hoaDonId, ArrayList<ChiTietHoaDon> dsMon)
            throws SQLException, LoiCSDL {
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
    }

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

    public void doiBan(int hoaDonId, int banMoiId) throws LoiCSDL {
        giaoDich(c -> {
            int banCu = khoaDonDangMo(c, hoaDonId)[0];
            Ban moi = layBan(c, banMoiId);
            if (moi.isCoNguoi()) throw new LoiCSDL("Bàn mới không trống");
            Ban cu = layBan(c, banCu);
            ghi(c, "UPDATE Orders SET table_id = ? WHERE id = ?", banMoiId, hoaDonId);
            ghi(c, "UPDATE `Tables` SET status = ? WHERE id = ?", Ban.maTrangThai(cu.getTrangThai()), banMoiId);
            return ghi(c, "UPDATE `Tables` SET status = 'EMPTY' WHERE id = ?", banCu);
        });
    }

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

    public HoaDon thanhToan(int hoaDonId, long giamGia, String phuongThuc, long khachDua) throws LoiCSDL {
        String maPhuongThuc = HoaDon.maPhuongThuc(phuongThuc);
        return giaoDich(c -> {
            int banId = khoaDonDangMo(c, hoaDonId)[0];
            long tamTinh = so(c, "SELECT COALESCE(SUM(quantity * unit_price), 0) FROM OrderDetails WHERE order_id = ?",
                    hoaDonId);
            if (tamTinh <= 0) throw new LoiCSDL("Đơn chưa có món để thanh toán");
            long giam = Math.max(0, Math.min(giamGia, tamTinh));
            long tong = TienIch.lamTron(tamTinh - giam);
            long nhan = "CASH".equals(maPhuongThuc) ? khachDua : tong;
            if (nhan < tong) throw new LoiCSDL("Khách đưa chưa đủ tiền");

            int hoaDonMoi = them(c, "INSERT INTO Invoices (order_id, user_id, subtotal, discount, total,"
                            + " payment_method, amount_received, change_amount) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    hoaDonId, nhanVien(c), tamTinh, giam, tong, maPhuongThuc, nhan, nhan - tong);
            ghi(c, "UPDATE Invoices SET invoice_code = CONCAT('HD', LPAD(id, 4, '0')) WHERE id = ?", hoaDonMoi);
            ghi(c, "UPDATE Orders SET status = 'PAID', closed_at = NOW() WHERE id = ?", hoaDonId);
            ghi(c, "UPDATE OrderDetails SET status = 'SERVED' WHERE order_id = ?", hoaDonId);
            if (banId > 0) ghi(c, "UPDATE `Tables` SET status = 'EMPTY' WHERE id = ?", banId);
            return layHoaDon(c, hoaDonId);
        });
    }
}
