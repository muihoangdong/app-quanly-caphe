package hutech.cntt.app_quanlycafe.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Date;
import java.util.Locale;

import hutech.cntt.app_quanlycafe.model.Ban;
import hutech.cntt.app_quanlycafe.model.ChiTietHoaDon;
import hutech.cntt.app_quanlycafe.model.DatBan;
import hutech.cntt.app_quanlycafe.model.DonOnline;
import hutech.cntt.app_quanlycafe.model.HoaDon;
import hutech.cntt.app_quanlycafe.model.TaiKhoan;
import hutech.cntt.app_quanlycafe.model.ThucUong;

/**
 * Cơ sở dữ liệu SQLite của quán: tài khoản, bàn, danh mục, thức uống, hóa đơn,
 * đặt bàn và đơn gọi món qua app của khách.
 */
public class CafeDatabase extends SQLiteOpenHelper {

    private static final String TEN_CSDL = "quanlycafe.db";
    private static final int PHIEN_BAN = 2;

    private static CafeDatabase instance;

    public static synchronized CafeDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new CafeDatabase(context.getApplicationContext());
        }
        return instance;
    }

    private CafeDatabase(Context context) {
        super(context, TEN_CSDL, null, PHIEN_BAN);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE ban (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ten TEXT NOT NULL," +
                "khu_vuc TEXT NOT NULL," +
                "so_cho INTEGER DEFAULT 4," +
                "trang_thai INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE thuc_uong (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ma TEXT," +
                "ten TEXT NOT NULL," +
                "danh_muc TEXT," +
                "mo_ta TEXT," +
                "gia INTEGER DEFAULT 0," +
                "gia_von INTEGER DEFAULT 0," +
                "hinh TEXT," +
                "con_ban INTEGER DEFAULT 1," +
                "ban_chay INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE hoa_don (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ma_hd TEXT," +
                "ban_id INTEGER," +
                "ten_ban TEXT," +
                "khu_vuc TEXT," +
                "so_khach INTEGER DEFAULT 1," +
                "gio_vao INTEGER," +
                "gio_ra INTEGER DEFAULT 0," +
                "trang_thai INTEGER DEFAULT 0," +
                "tam_tinh INTEGER DEFAULT 0," +
                "giam_gia INTEGER DEFAULT 0," +
                "vat INTEGER DEFAULT 0," +
                "tong_tien INTEGER DEFAULT 0," +
                "phuong_thuc TEXT," +
                "khach_dua INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE chi_tiet (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "hoa_don_id INTEGER," +
                "thuc_uong_id INTEGER," +
                "ten_mon TEXT," +
                "don_gia INTEGER," +
                "so_luong INTEGER," +
                "ghi_chu TEXT," +
                "trang_thai INTEGER DEFAULT 0," +
                "thoi_gian INTEGER)");

        db.execSQL("CREATE TABLE tai_khoan (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ten_dang_nhap TEXT NOT NULL UNIQUE," +
                "mat_khau TEXT NOT NULL," +
                "ho_ten TEXT NOT NULL," +
                "so_dien_thoai TEXT," +
                "email TEXT," +
                "vai_tro INTEGER DEFAULT 0," +
                "chuc_vu TEXT," +
                "dang_hoat_dong INTEGER DEFAULT 1," +
                "ngay_tao INTEGER)");

        db.execSQL("CREATE TABLE danh_muc (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ten TEXT NOT NULL UNIQUE)");

        db.execSQL("CREATE TABLE dat_ban (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ma_dat TEXT," +
                "khach_id INTEGER," +
                "ban_id INTEGER," +
                "ten_ban TEXT," +
                "khu_vuc TEXT," +
                "so_khach INTEGER DEFAULT 2," +
                "thoi_gian INTEGER," +
                "ghi_chu TEXT," +
                "trang_thai INTEGER DEFAULT 0," +
                "ngay_tao INTEGER," +
                "hoa_don_id INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE don_online (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ma_don TEXT," +
                "khach_id INTEGER," +
                "dat_ban_id INTEGER DEFAULT 0," +
                "hinh_thuc TEXT," +
                "phuong_thuc TEXT," +
                "da_thanh_toan INTEGER DEFAULT 0," +
                "trang_thai INTEGER DEFAULT 0," +
                "tong_tien INTEGER DEFAULT 0," +
                "ghi_chu TEXT," +
                "thoi_gian INTEGER)");

        db.execSQL("CREATE TABLE don_online_ct (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "don_id INTEGER," +
                "thuc_uong_id INTEGER," +
                "ten_mon TEXT," +
                "don_gia INTEGER," +
                "so_luong INTEGER," +
                "ghi_chu TEXT)");

        taoDuLieuMau(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS don_online_ct");
        db.execSQL("DROP TABLE IF EXISTS don_online");
        db.execSQL("DROP TABLE IF EXISTS dat_ban");
        db.execSQL("DROP TABLE IF EXISTS danh_muc");
        db.execSQL("DROP TABLE IF EXISTS tai_khoan");
        db.execSQL("DROP TABLE IF EXISTS chi_tiet");
        db.execSQL("DROP TABLE IF EXISTS hoa_don");
        db.execSQL("DROP TABLE IF EXISTS thuc_uong");
        db.execSQL("DROP TABLE IF EXISTS ban");
        onCreate(db);
    }

    // ===================== DỮ LIỆU MẪU =====================

    private void taoDuLieuMau(SQLiteDatabase db) {
        // Tài khoản mẫu: admin / admin123, nhanvien / 123456, khách 0909123456 / 123456
        themTaiKhoanMau(db, "admin", "admin123", "Trần Quản Lý", "0901000001",
                TaiKhoan.QUAN_TRI, "Quản lý cửa hàng");
        themTaiKhoanMau(db, "nhanvien", "123456", "Lê Thu Ngân", "0901000002",
                TaiKhoan.NHAN_VIEN, "Thu ngân");
        themTaiKhoanMau(db, "phache", "123456", "Phạm Pha Chế", "0901000003",
                TaiKhoan.NHAN_VIEN, "Pha chế");
        int khach = themTaiKhoanMau(db, "0909123456", "123456", "Nguyễn Minh Anh", "0909123456",
                TaiKhoan.KHACH_HANG, null);

        themBan(db, "Bàn 01", "Tầng 1", 4);
        themBan(db, "Bàn 02", "Tầng 1", 2);
        themBan(db, "Bàn 03", "Tầng 1", 4);
        themBan(db, "Bàn 04", "Tầng 1", 2);
        themBan(db, "Bàn 05", "Tầng 2", 4);
        themBan(db, "Bàn 06", "Tầng 2", 6);
        themBan(db, "Vườn 01", "Sân vườn", 8);
        themBan(db, "Vườn 02", "Sân vườn", 4);

        themMonMau(db, "CF-MUOI-01", "Cà phê Muối Đặc Biệt", "Cà phê truyền thống",
                "Kem béo bồng bềnh, cà phê Robusta Đắk Lắk đậm vị", 39000, 12000, true);
        themMonMau(db, "CF-BACXIU", "Bạc Xỉu Sữa Dừa", "Cà phê truyền thống",
                "Sữa dừa béo bùi, cà phê dịu nhẹ", 35000, 11000, false);
        themMonMau(db, "CF-DEN-DA", "Cà phê Đen Đá", "Cà phê truyền thống",
                "Phin truyền thống, đậm đà", 25000, 7000, false);
        themMonMau(db, "ES-TONIC", "Espresso Tonic Cam", "Cà phê pha máy",
                "Sủi bọt tonic, sảng khoái với cam vàng", 45000, 15000, false);
        themMonMau(db, "ES-LATTE", "Caramel Latte", "Cà phê pha máy",
                "Espresso, sữa tươi và sốt caramel", 49000, 16000, true);
        themMonMau(db, "ES-AMERI", "Americano", "Cà phê pha máy",
                "Espresso pha loãng, thanh nhẹ", 35000, 9000, false);
        themMonMau(db, "TR-DAO", "Trà Đào Cam Sả", "Trà trái cây",
                "Đào giòn sần sật, sả thanh mát", 42000, 13000, true);
        themMonMau(db, "TR-MANGCAU", "Trà Mãng Cầu Đắk Lắk", "Trà trái cây",
                "Chua ngọt tự nhiên, thịt quả tươi", 40000, 12500, false);
        themMonMau(db, "DX-MATCHA", "Matcha Đá Xay", "Đá xay",
                "Matcha Nhật, kem tươi béo ngậy", 55000, 18000, false);
        themMonMau(db, "DX-COOKIE", "Cookie Đá Xay", "Đá xay",
                "Bánh quy sô-cô-la xay cùng sữa", 52000, 17000, false);
        themMonMau(db, "BK-CROISS", "Croissant Bơ Tỏi", "Bánh & Snack",
                "Vỏ ngàn lớp giòn, thơm nức bơ tỏi", 35000, 14000, false);
        themMonMau(db, "BK-TIRAMI", "Bánh Tiramisu", "Bánh & Snack",
                "Mềm mịn, thơm mùi cà phê và cacao", 45000, 18000, false);

        long now = System.currentTimeMillis();
        long phut = 60 * 1000L;

        // Bàn 01 đang phục vụ
        int hd1 = moBan(db, 1, "Bàn 01", "Tầng 1", 4, now - 45 * phut);
        themChiTietMau(db, hd1, 1, "Cà phê Muối Đặc Biệt", 39000, 2, ChiTietHoaDon.DA_LEN_MON, now - 40 * phut);
        themChiTietMau(db, hd1, 7, "Trà Đào Cam Sả", 42000, 1, ChiTietHoaDon.DANG_PHA_CHE, now - 10 * phut);
        themChiTietMau(db, hd1, 11, "Croissant Bơ Tỏi", 35000, 1, ChiTietHoaDon.DA_LEN_MON, now - 38 * phut);

        // Bàn 03 chờ thanh toán
        int hd3 = moBan(db, 3, "Bàn 03", "Tầng 1", 3, now - 70 * phut);
        themChiTietMau(db, hd3, 5, "Caramel Latte", 49000, 3, ChiTietHoaDon.DA_LEN_MON, now - 65 * phut);
        themChiTietMau(db, hd3, 12, "Bánh Tiramisu", 45000, 2, ChiTietHoaDon.DA_LEN_MON, now - 60 * phut);
        capNhatTrangThaiBan(db, 3, Ban.CHO_THANH_TOAN);

        // Vườn 01 đang phục vụ
        int hd7 = moBan(db, 7, "Vườn 01", "Sân vườn", 6, now - 70 * phut);
        themChiTietMau(db, hd7, 4, "Espresso Tonic Cam", 45000, 2, ChiTietHoaDon.DA_LEN_MON, now - 65 * phut);
        themChiTietMau(db, hd7, 9, "Matcha Đá Xay", 55000, 2, ChiTietHoaDon.DANG_PHA_CHE, now - 5 * phut);
        themChiTietMau(db, hd7, 2, "Bạc Xỉu Sữa Dừa", 35000, 3, ChiTietHoaDon.DA_LEN_MON, now - 60 * phut);

        // Một vài hóa đơn đã thanh toán trong ngày để có số liệu báo cáo
        hoaDonDaThanhToanMau(db, 2, "Bàn 02", "Tầng 1", now - 300 * phut, "VietQR",
                new int[]{1, 3}, new String[]{"Cà phê Muối Đặc Biệt", "Cà phê Đen Đá"},
                new long[]{39000, 25000}, new int[]{2, 1});
        hoaDonDaThanhToanMau(db, 5, "Bàn 05", "Tầng 2", now - 240 * phut, "Tiền mặt",
                new int[]{5, 7, 11}, new String[]{"Caramel Latte", "Trà Đào Cam Sả", "Croissant Bơ Tỏi"},
                new long[]{49000, 42000, 35000}, new int[]{2, 2, 1});
        hoaDonDaThanhToanMau(db, 6, "Bàn 06", "Tầng 2", now - 180 * phut, "Quẹt POS",
                new int[]{1, 4, 12}, new String[]{"Cà phê Muối Đặc Biệt", "Espresso Tonic Cam", "Bánh Tiramisu"},
                new long[]{39000, 45000, 45000}, new int[]{3, 1, 2});
        hoaDonDaThanhToanMau(db, 8, "Vườn 02", "Sân vườn", now - 120 * phut, "Ví MoMo/Zalo",
                new int[]{8, 2}, new String[]{"Trà Mãng Cầu Đắk Lắk", "Bạc Xỉu Sữa Dừa"},
                new long[]{40000, 35000}, new int[]{2, 2});

        // Khách mẫu đã đặt Bàn 06 lúc 19:00 ngày mai và gọi trước 2 món
        Calendar mai = Calendar.getInstance();
        mai.add(Calendar.DAY_OF_MONTH, 1);
        mai.set(Calendar.HOUR_OF_DAY, 19);
        mai.set(Calendar.MINUTE, 0);
        mai.set(Calendar.SECOND, 0);
        mai.set(Calendar.MILLISECOND, 0);
        ContentValues dat = new ContentValues();
        dat.put("khach_id", khach);
        dat.put("ban_id", 6);
        dat.put("ten_ban", "Bàn 06");
        dat.put("khu_vuc", "Tầng 2");
        dat.put("so_khach", 4);
        dat.put("thoi_gian", mai.getTimeInMillis());
        dat.put("ghi_chu", "Sinh nhật bạn, cần bàn gần cửa sổ");
        dat.put("trang_thai", DatBan.CHO_DEN);
        dat.put("ngay_tao", now);
        int datId = (int) db.insert("dat_ban", null, dat);
        ganMa(db, "dat_ban", "ma_dat", "DB", datId);

        ContentValues don = new ContentValues();
        don.put("khach_id", khach);
        don.put("dat_ban_id", datId);
        don.put("hinh_thuc", DonOnline.TAI_QUAN);
        don.put("phuong_thuc", "Ví MoMo");
        don.put("da_thanh_toan", 1);
        don.put("trang_thai", DonOnline.CHO_XAC_NHAN);
        don.put("tong_tien", 39000 * 2 + 45000);
        don.put("ghi_chu", "Lên món khi khách tới");
        don.put("thoi_gian", now);
        int donId = (int) db.insert("don_online", null, don);
        ganMa(db, "don_online", "ma_don", "DO", donId);
        themDongDon(db, donId, 1, "Cà phê Muối Đặc Biệt", 39000, 2, "");
        themDongDon(db, donId, 12, "Bánh Tiramisu", 45000, 1, "");
    }

    private int themTaiKhoanMau(SQLiteDatabase db, String tenDangNhap, String matKhau, String hoTen,
                                String sdt, int vaiTro, String chucVu) {
        ContentValues cv = new ContentValues();
        cv.put("ten_dang_nhap", tenDangNhap);
        cv.put("mat_khau", PhienDangNhap.bamMatKhau(matKhau));
        cv.put("ho_ten", hoTen);
        cv.put("so_dien_thoai", sdt);
        cv.put("vai_tro", vaiTro);
        cv.put("chuc_vu", chucVu);
        cv.put("dang_hoat_dong", 1);
        cv.put("ngay_tao", System.currentTimeMillis());
        return (int) db.insert("tai_khoan", null, cv);
    }

    private void themDongDon(SQLiteDatabase db, int donId, int thucUongId, String ten, long gia,
                             int soLuong, String ghiChu) {
        ContentValues cv = new ContentValues();
        cv.put("don_id", donId);
        cv.put("thuc_uong_id", thucUongId);
        cv.put("ten_mon", ten);
        cv.put("don_gia", gia);
        cv.put("so_luong", soLuong);
        cv.put("ghi_chu", ghiChu == null ? "" : ghiChu);
        db.insert("don_online_ct", null, cv);
    }

    /** Sinh mã dạng DB-20261021-0001 cho dòng vừa thêm. */
    private String ganMa(SQLiteDatabase db, String bang, String cot, String tienTo, int id) {
        String ngay = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String ma = tienTo + "-" + ngay + "-" + String.format(Locale.getDefault(), "%04d", id);
        ContentValues cv = new ContentValues();
        cv.put(cot, ma);
        db.update(bang, cv, "id=?", new String[]{String.valueOf(id)});
        return ma;
    }

    private void themBan(SQLiteDatabase db, String ten, String khuVuc, int soCho) {
        ContentValues cv = new ContentValues();
        cv.put("ten", ten);
        cv.put("khu_vuc", khuVuc);
        cv.put("so_cho", soCho);
        cv.put("trang_thai", Ban.TRONG);
        db.insert("ban", null, cv);
    }

    private void themMonMau(SQLiteDatabase db, String ma, String ten, String danhMuc, String moTa,
                            long gia, long giaVon, boolean banChay) {
        ContentValues cv = new ContentValues();
        cv.put("ma", ma);
        cv.put("ten", ten);
        cv.put("danh_muc", danhMuc);
        cv.put("mo_ta", moTa);
        cv.put("gia", gia);
        cv.put("gia_von", giaVon);
        cv.put("con_ban", 1);
        cv.put("ban_chay", banChay ? 1 : 0);
        db.insert("thuc_uong", null, cv);
        themDanhMuc(db, danhMuc);
    }

    private void themChiTietMau(SQLiteDatabase db, int hoaDonId, int thucUongId, String ten,
                                long donGia, int soLuong, int trangThai, long thoiGian) {
        ContentValues cv = new ContentValues();
        cv.put("hoa_don_id", hoaDonId);
        cv.put("thuc_uong_id", thucUongId);
        cv.put("ten_mon", ten);
        cv.put("don_gia", donGia);
        cv.put("so_luong", soLuong);
        cv.put("ghi_chu", "");
        cv.put("trang_thai", trangThai);
        cv.put("thoi_gian", thoiGian);
        db.insert("chi_tiet", null, cv);
    }

    private void hoaDonDaThanhToanMau(SQLiteDatabase db, int banId, String tenBan, String khuVuc,
                                      long gioVao, String phuongThuc, int[] monIds, String[] tenMon,
                                      long[] gia, int[] soLuong) {
        int hd = moBan(db, banId, tenBan, khuVuc, 2, gioVao);
        long tamTinh = 0;
        for (int i = 0; i < monIds.length; i++) {
            themChiTietMau(db, hd, monIds[i], tenMon[i], gia[i], soLuong[i], ChiTietHoaDon.DA_LEN_MON, gioVao);
            tamTinh += gia[i] * soLuong[i];
        }
        long vat = Math.round(tamTinh * 0.08);
        long tong = lamTron(tamTinh + vat);
        ContentValues cv = new ContentValues();
        cv.put("trang_thai", HoaDon.DA_THANH_TOAN);
        cv.put("gio_ra", gioVao + 50 * 60 * 1000L);
        cv.put("tam_tinh", tamTinh);
        cv.put("vat", vat);
        cv.put("tong_tien", tong);
        cv.put("khach_dua", tong);
        cv.put("phuong_thuc", phuongThuc);
        db.update("hoa_don", cv, "id=?", new String[]{String.valueOf(hd)});
        capNhatTrangThaiBan(db, banId, Ban.TRONG);
    }

    /** Làm tròn tới hàng nghìn đồng. */
    public static long lamTron(long soTien) {
        return Math.round(soTien / 1000.0) * 1000;
    }

    // ===================== BÀN =====================

    public ArrayList<Ban> layDanhSachBan(String khuVuc) {
        ArrayList<Ban> ds = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT b.id, b.ten, b.khu_vuc, b.so_cho, b.trang_thai," +
                " h.id, h.so_khach, h.gio_vao," +
                " (SELECT IFNULL(SUM(so_luong),0) FROM chi_tiet WHERE hoa_don_id = h.id)," +
                " (SELECT IFNULL(SUM(so_luong*don_gia),0) FROM chi_tiet WHERE hoa_don_id = h.id)" +
                " FROM ban b LEFT JOIN hoa_don h ON h.ban_id = b.id AND h.trang_thai = " + HoaDon.DANG_PHUC_VU;
        String[] args = null;
        if (khuVuc != null) {
            sql += " WHERE b.khu_vuc = ?";
            args = new String[]{khuVuc};
        }
        sql += " ORDER BY b.id";
        Cursor c = db.rawQuery(sql, args);
        while (c.moveToNext()) {
            Ban ban = new Ban(c.getInt(0), c.getString(1), c.getString(2), c.getInt(3), c.getInt(4));
            if (!c.isNull(5)) {
                ban.setHoaDonId(c.getInt(5));
                ban.setSoKhach(c.getInt(6));
                ban.setGioVao(c.getLong(7));
                ban.setSoMon(c.getInt(8));
                ban.setTamTinh(c.getLong(9));
            }
            ds.add(ban);
        }
        c.close();
        return ds;
    }

    public ArrayList<String> layDanhSachKhuVuc() {
        ArrayList<String> ds = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT DISTINCT khu_vuc FROM ban ORDER BY khu_vuc", null);
        while (c.moveToNext()) ds.add(c.getString(0));
        c.close();
        return ds;
    }

    public void doiTenKhuVuc(String tenCu, String tenMoi) {
        ContentValues cv = new ContentValues();
        cv.put("khu_vuc", tenMoi);
        getWritableDatabase().update("ban", cv, "khu_vuc=?", new String[]{tenCu});
    }

    public int demBan(String khuVuc) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM ban WHERE khu_vuc = ?", new String[]{khuVuc});
        int dem = c.moveToFirst() ? c.getInt(0) : 0;
        c.close();
        return dem;
    }

    public long themBan(Ban ban) {
        ContentValues cv = new ContentValues();
        cv.put("ten", ban.getTenBan());
        cv.put("khu_vuc", ban.getKhuVuc());
        cv.put("so_cho", ban.getSoCho());
        cv.put("trang_thai", Ban.TRONG);
        return getWritableDatabase().insert("ban", null, cv);
    }

    public void suaBan(Ban ban) {
        ContentValues cv = new ContentValues();
        cv.put("ten", ban.getTenBan());
        cv.put("khu_vuc", ban.getKhuVuc());
        cv.put("so_cho", ban.getSoCho());
        getWritableDatabase().update("ban", cv, "id=?", new String[]{String.valueOf(ban.getId())});
    }

    public void xoaBan(int banId) {
        getWritableDatabase().delete("ban", "id=?", new String[]{String.valueOf(banId)});
    }

    public void capNhatTrangThaiBan(int banId, int trangThai) {
        capNhatTrangThaiBan(getWritableDatabase(), banId, trangThai);
    }

    private void capNhatTrangThaiBan(SQLiteDatabase db, int banId, int trangThai) {
        ContentValues cv = new ContentValues();
        cv.put("trang_thai", trangThai);
        db.update("ban", cv, "id=?", new String[]{String.valueOf(banId)});
    }

    // ===================== THỨC UỐNG =====================

    public ArrayList<ThucUong> layDanhSachThucUong(String danhMuc, String tuKhoa, boolean chiMonConBan) {
        ArrayList<ThucUong> ds = new ArrayList<>();
        StringBuilder where = new StringBuilder("1=1");
        ArrayList<String> args = new ArrayList<>();
        if (danhMuc != null) {
            where.append(" AND danh_muc = ?");
            args.add(danhMuc);
        }
        if (tuKhoa != null && !tuKhoa.trim().isEmpty()) {
            where.append(" AND (ten LIKE ? OR ma LIKE ?)");
            args.add("%" + tuKhoa.trim() + "%");
            args.add("%" + tuKhoa.trim() + "%");
        }
        if (chiMonConBan) {
            where.append(" AND con_ban = 1");
        }
        Cursor c = getReadableDatabase().query("thuc_uong", null, where.toString(),
                args.toArray(new String[0]), null, null, "ban_chay DESC, danh_muc, ten");
        while (c.moveToNext()) ds.add(docThucUong(c));
        c.close();
        return ds;
    }

    public ThucUong layThucUong(int id) {
        Cursor c = getReadableDatabase().query("thuc_uong", null, "id=?",
                new String[]{String.valueOf(id)}, null, null, null);
        ThucUong tu = c.moveToFirst() ? docThucUong(c) : null;
        c.close();
        return tu;
    }

    private ThucUong docThucUong(Cursor c) {
        return new ThucUong(
                c.getInt(c.getColumnIndexOrThrow("id")),
                c.getString(c.getColumnIndexOrThrow("ma")),
                c.getString(c.getColumnIndexOrThrow("ten")),
                c.getString(c.getColumnIndexOrThrow("danh_muc")),
                c.getString(c.getColumnIndexOrThrow("mo_ta")),
                c.getLong(c.getColumnIndexOrThrow("gia")),
                c.getLong(c.getColumnIndexOrThrow("gia_von")),
                c.getString(c.getColumnIndexOrThrow("hinh")),
                c.getInt(c.getColumnIndexOrThrow("con_ban")) == 1,
                c.getInt(c.getColumnIndexOrThrow("ban_chay")) == 1);
    }

    // ===================== DANH MỤC =====================

    public ArrayList<String> layDanhSachDanhMuc() {
        ArrayList<String> ds = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT ten FROM danh_muc ORDER BY id", null);
        while (c.moveToNext()) ds.add(c.getString(0));
        c.close();
        return ds;
    }

    private void themDanhMuc(SQLiteDatabase db, String ten) {
        if (ten == null || ten.trim().isEmpty()) return;
        ContentValues cv = new ContentValues();
        cv.put("ten", ten.trim());
        db.insertWithOnConflict("danh_muc", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    /** Trả về false nếu tên đã tồn tại. */
    public boolean themDanhMuc(String ten) {
        if (layDanhSachDanhMuc().contains(ten)) return false;
        themDanhMuc(getWritableDatabase(), ten);
        return true;
    }

    /** Đổi tên danh mục và cập nhật luôn các món thuộc danh mục đó. */
    public boolean doiTenDanhMuc(String tenCu, String tenMoi) {
        if (layDanhSachDanhMuc().contains(tenMoi)) return false;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("ten", tenMoi);
        db.update("danh_muc", cv, "ten=?", new String[]{tenCu});
        ContentValues mon = new ContentValues();
        mon.put("danh_muc", tenMoi);
        db.update("thuc_uong", mon, "danh_muc=?", new String[]{tenCu});
        return true;
    }

    public int demMonTrongDanhMuc(String ten) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM thuc_uong WHERE danh_muc = ?", new String[]{ten});
        int dem = c.moveToFirst() ? c.getInt(0) : 0;
        c.close();
        return dem;
    }

    /** Chỉ xóa được danh mục chưa có món nào. */
    public boolean xoaDanhMuc(String ten) {
        if (demMonTrongDanhMuc(ten) > 0) return false;
        getWritableDatabase().delete("danh_muc", "ten=?", new String[]{ten});
        return true;
    }

    private ContentValues giaTriThucUong(ThucUong tu) {
        ContentValues cv = new ContentValues();
        cv.put("ma", tu.getMaMon());
        cv.put("ten", tu.getTenMon());
        cv.put("danh_muc", tu.getDanhMuc());
        cv.put("mo_ta", tu.getMoTa());
        cv.put("gia", tu.getGia());
        cv.put("gia_von", tu.getGiaVon());
        cv.put("hinh", tu.getUriAnh());
        cv.put("con_ban", tu.isConBan() ? 1 : 0);
        cv.put("ban_chay", tu.isBanChay() ? 1 : 0);
        return cv;
    }

    public long themThucUong(ThucUong tu) {
        themDanhMuc(getWritableDatabase(), tu.getDanhMuc());
        return getWritableDatabase().insert("thuc_uong", null, giaTriThucUong(tu));
    }

    public void suaThucUong(ThucUong tu) {
        themDanhMuc(getWritableDatabase(), tu.getDanhMuc());
        getWritableDatabase().update("thuc_uong", giaTriThucUong(tu), "id=?",
                new String[]{String.valueOf(tu.getId())});
    }

    public void xoaThucUong(int id) {
        getWritableDatabase().delete("thuc_uong", "id=?", new String[]{String.valueOf(id)});
    }

    // ===================== HÓA ĐƠN / GỌI MÓN =====================

    /** Mở bàn: tạo hóa đơn mới ở trạng thái đang phục vụ, trả về id hóa đơn. */
    public int moBan(Ban ban, int soKhach) {
        return moBan(getWritableDatabase(), ban.getId(), ban.getTenBan(), ban.getKhuVuc(),
                soKhach, System.currentTimeMillis());
    }

    private int moBan(SQLiteDatabase db, int banId, String tenBan, String khuVuc, int soKhach, long gioVao) {
        ContentValues cv = new ContentValues();
        cv.put("ban_id", banId);
        cv.put("ten_ban", tenBan);
        cv.put("khu_vuc", khuVuc);
        cv.put("so_khach", soKhach);
        cv.put("gio_vao", gioVao);
        cv.put("trang_thai", HoaDon.DANG_PHUC_VU);
        int id = (int) db.insert("hoa_don", null, cv);

        String ngay = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date(gioVao));
        ContentValues ma = new ContentValues();
        ma.put("ma_hd", "HD-" + ngay + "-" + String.format(Locale.getDefault(), "%04d", id));
        db.update("hoa_don", ma, "id=?", new String[]{String.valueOf(id)});

        capNhatTrangThaiBan(db, banId, Ban.CO_KHACH);
        return id;
    }

    public HoaDon layHoaDon(int id) {
        ArrayList<HoaDon> ds = truyVanHoaDon("h.id = ?", new String[]{String.valueOf(id)});
        return ds.isEmpty() ? null : ds.get(0);
    }

    /** trangThai = -1 để lấy tất cả. */
    public ArrayList<HoaDon> layDanhSachHoaDon(int trangThai) {
        if (trangThai < 0) return truyVanHoaDon(null, null);
        return truyVanHoaDon("h.trang_thai = ?", new String[]{String.valueOf(trangThai)});
    }

    public ArrayList<HoaDon> layHoaDonDaThanhToan(long tuNgay, long denNgay) {
        return truyVanHoaDon("h.trang_thai = " + HoaDon.DA_THANH_TOAN + " AND h.gio_ra >= ? AND h.gio_ra < ?",
                new String[]{String.valueOf(tuNgay), String.valueOf(denNgay)});
    }

    private ArrayList<HoaDon> truyVanHoaDon(String where, String[] args) {
        ArrayList<HoaDon> ds = new ArrayList<>();
        String sql = "SELECT h.id, h.ma_hd, h.ban_id, h.ten_ban, h.khu_vuc, h.so_khach, h.gio_vao, h.gio_ra," +
                " h.trang_thai, h.tam_tinh, h.giam_gia, h.vat, h.tong_tien, h.phuong_thuc, h.khach_dua," +
                " (SELECT IFNULL(SUM(so_luong),0) FROM chi_tiet WHERE hoa_don_id = h.id)," +
                " (SELECT IFNULL(SUM(so_luong*don_gia),0) FROM chi_tiet WHERE hoa_don_id = h.id)" +
                " FROM hoa_don h";
        if (where != null) sql += " WHERE " + where;
        sql += " ORDER BY h.trang_thai = 0 DESC, MAX(h.gio_vao, h.gio_ra) DESC";
        Cursor c = getReadableDatabase().rawQuery(sql, args);
        while (c.moveToNext()) {
            HoaDon hd = new HoaDon();
            hd.setId(c.getInt(0));
            hd.setMaHoaDon(c.getString(1));
            hd.setBanId(c.getInt(2));
            hd.setTenBan(c.getString(3));
            hd.setKhuVuc(c.getString(4));
            hd.setSoKhach(c.getInt(5));
            hd.setGioVao(c.getLong(6));
            hd.setGioRa(c.getLong(7));
            hd.setTrangThai(c.getInt(8));
            hd.setGiamGia(c.getLong(10));
            hd.setVat(c.getLong(11));
            hd.setTongTien(c.getLong(12));
            hd.setPhuongThuc(c.getString(13));
            hd.setKhachDua(c.getLong(14));
            hd.setSoMon(c.getInt(15));
            // Hóa đơn đang phục vụ thì tạm tính lấy theo chi tiết hiện tại
            hd.setTamTinh(hd.getTrangThai() == HoaDon.DANG_PHUC_VU ? c.getLong(16) : c.getLong(9));
            ds.add(hd);
        }
        c.close();
        return ds;
    }

    public ArrayList<ChiTietHoaDon> layChiTietHoaDon(int hoaDonId) {
        ArrayList<ChiTietHoaDon> ds = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT ct.id, ct.hoa_don_id, ct.thuc_uong_id, ct.ten_mon, ct.don_gia, ct.so_luong," +
                        " ct.ghi_chu, ct.trang_thai, ct.thoi_gian, t.danh_muc, t.hinh" +
                        " FROM chi_tiet ct LEFT JOIN thuc_uong t ON t.id = ct.thuc_uong_id" +
                        " WHERE ct.hoa_don_id = ? ORDER BY ct.trang_thai, ct.thoi_gian",
                new String[]{String.valueOf(hoaDonId)});
        while (c.moveToNext()) {
            ChiTietHoaDon ct = new ChiTietHoaDon();
            ct.setId(c.getInt(0));
            ct.setHoaDonId(c.getInt(1));
            ct.setThucUongId(c.getInt(2));
            ct.setTenMon(c.getString(3));
            ct.setDonGia(c.getLong(4));
            ct.setSoLuong(c.getInt(5));
            ct.setGhiChu(c.getString(6));
            ct.setTrangThai(c.getInt(7));
            ct.setThoiGian(c.getLong(8));
            ct.setDanhMuc(c.getString(9));
            ct.setUriAnh(c.getString(10));
            ds.add(ct);
        }
        c.close();
        return ds;
    }

    /** Gửi món xuống quầy bar: gộp vào dòng đang pha chế cùng món nếu có. */
    public void themMonVaoHoaDon(int hoaDonId, ThucUong tu, int soLuong, String ghiChu) {
        SQLiteDatabase db = getWritableDatabase();
        String gc = ghiChu == null ? "" : ghiChu;
        Cursor c = db.rawQuery("SELECT id, so_luong FROM chi_tiet WHERE hoa_don_id = ? AND thuc_uong_id = ?" +
                        " AND trang_thai = ? AND IFNULL(ghi_chu,'') = ?",
                new String[]{String.valueOf(hoaDonId), String.valueOf(tu.getId()),
                        String.valueOf(ChiTietHoaDon.DANG_PHA_CHE), gc});
        if (c.moveToFirst()) {
            capNhatSoLuong(c.getInt(0), c.getInt(1) + soLuong);
        } else {
            ContentValues cv = new ContentValues();
            cv.put("hoa_don_id", hoaDonId);
            cv.put("thuc_uong_id", tu.getId());
            cv.put("ten_mon", tu.getTenMon());
            cv.put("don_gia", tu.getGia());
            cv.put("so_luong", soLuong);
            cv.put("ghi_chu", gc);
            cv.put("trang_thai", ChiTietHoaDon.DANG_PHA_CHE);
            cv.put("thoi_gian", System.currentTimeMillis());
            db.insert("chi_tiet", null, cv);
        }
        c.close();
    }

    public void capNhatSoLuong(int chiTietId, int soLuong) {
        if (soLuong <= 0) {
            xoaChiTiet(chiTietId);
            return;
        }
        ContentValues cv = new ContentValues();
        cv.put("so_luong", soLuong);
        getWritableDatabase().update("chi_tiet", cv, "id=?", new String[]{String.valueOf(chiTietId)});
    }

    public void xoaChiTiet(int chiTietId) {
        getWritableDatabase().delete("chi_tiet", "id=?", new String[]{String.valueOf(chiTietId)});
    }

    public void capNhatTrangThaiMon(int chiTietId, int trangThai) {
        ContentValues cv = new ContentValues();
        cv.put("trang_thai", trangThai);
        getWritableDatabase().update("chi_tiet", cv, "id=?", new String[]{String.valueOf(chiTietId)});
    }

    public void thanhToan(HoaDon hd) {
        ContentValues cv = new ContentValues();
        cv.put("trang_thai", HoaDon.DA_THANH_TOAN);
        cv.put("gio_ra", System.currentTimeMillis());
        cv.put("tam_tinh", hd.getTamTinh());
        cv.put("giam_gia", hd.getGiamGia());
        cv.put("vat", hd.getVat());
        cv.put("tong_tien", hd.getTongTien());
        cv.put("phuong_thuc", hd.getPhuongThuc());
        cv.put("khach_dua", hd.getKhachDua());
        SQLiteDatabase db = getWritableDatabase();
        db.update("hoa_don", cv, "id=?", new String[]{String.valueOf(hd.getId())});
        // Tất cả món coi như đã lên
        ContentValues mon = new ContentValues();
        mon.put("trang_thai", ChiTietHoaDon.DA_LEN_MON);
        db.update("chi_tiet", mon, "hoa_don_id=?", new String[]{String.valueOf(hd.getId())});
        capNhatTrangThaiBan(db, hd.getBanId(), Ban.TRONG);
    }

    public void huyHoaDon(HoaDon hd) {
        ContentValues cv = new ContentValues();
        cv.put("trang_thai", HoaDon.DA_HUY);
        cv.put("gio_ra", System.currentTimeMillis());
        SQLiteDatabase db = getWritableDatabase();
        db.update("hoa_don", cv, "id=?", new String[]{String.valueOf(hd.getId())});
        capNhatTrangThaiBan(db, hd.getBanId(), Ban.TRONG);
    }

    /** Chuyển hóa đơn đang mở từ bàn cũ sang bàn mới (bàn mới phải trống). */
    public void doiBan(int hoaDonId, Ban banCu, Ban banMoi) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("ban_id", banMoi.getId());
        cv.put("ten_ban", banMoi.getTenBan());
        cv.put("khu_vuc", banMoi.getKhuVuc());
        db.update("hoa_don", cv, "id=?", new String[]{String.valueOf(hoaDonId)});
        capNhatTrangThaiBan(db, banMoi.getId(), banCu.getTrangThai());
        capNhatTrangThaiBan(db, banCu.getId(), Ban.TRONG);
    }

    /** Gộp toàn bộ món của bàn nguồn sang bàn đích, bàn nguồn trở thành bàn trống. */
    public void gopBan(Ban nguon, Ban dich) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("hoa_don_id", dich.getHoaDonId());
        db.update("chi_tiet", cv, "hoa_don_id=?", new String[]{String.valueOf(nguon.getHoaDonId())});

        ContentValues khach = new ContentValues();
        khach.put("so_khach", nguon.getSoKhach() + dich.getSoKhach());
        db.update("hoa_don", khach, "id=?", new String[]{String.valueOf(dich.getHoaDonId())});

        db.delete("hoa_don", "id=?", new String[]{String.valueOf(nguon.getHoaDonId())});
        capNhatTrangThaiBan(db, nguon.getId(), Ban.TRONG);
    }

    // ===================== BÁO CÁO =====================

    /** Mỗi phần tử: {tên món, số lượng, doanh thu}. */
    public ArrayList<Object[]> layMonBanChay(long tuNgay, long denNgay, int gioiHan) {
        ArrayList<Object[]> ds = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT ct.ten_mon, SUM(ct.so_luong) sl, SUM(ct.so_luong*ct.don_gia) dt" +
                        " FROM chi_tiet ct JOIN hoa_don h ON h.id = ct.hoa_don_id" +
                        " WHERE h.trang_thai = ? AND h.gio_ra >= ? AND h.gio_ra < ?" +
                        " GROUP BY ct.ten_mon ORDER BY sl DESC, dt DESC LIMIT " + gioiHan,
                new String[]{String.valueOf(HoaDon.DA_THANH_TOAN), String.valueOf(tuNgay), String.valueOf(denNgay)});
        while (c.moveToNext()) {
            ds.add(new Object[]{c.getString(0), c.getInt(1), c.getLong(2)});
        }
        c.close();
        return ds;
    }

    // ===================== TÀI KHOẢN =====================

    private TaiKhoan docTaiKhoan(Cursor c) {
        TaiKhoan tk = new TaiKhoan();
        tk.setId(c.getInt(c.getColumnIndexOrThrow("id")));
        tk.setTenDangNhap(c.getString(c.getColumnIndexOrThrow("ten_dang_nhap")));
        tk.setHoTen(c.getString(c.getColumnIndexOrThrow("ho_ten")));
        tk.setSoDienThoai(c.getString(c.getColumnIndexOrThrow("so_dien_thoai")));
        tk.setEmail(c.getString(c.getColumnIndexOrThrow("email")));
        tk.setVaiTro(c.getInt(c.getColumnIndexOrThrow("vai_tro")));
        tk.setChucVu(c.getString(c.getColumnIndexOrThrow("chuc_vu")));
        tk.setDangHoatDong(c.getInt(c.getColumnIndexOrThrow("dang_hoat_dong")) == 1);
        tk.setNgayTao(c.getLong(c.getColumnIndexOrThrow("ngay_tao")));
        return tk;
    }

    /**
     * Đăng nhập bằng tên đăng nhập hoặc số điện thoại.
     * nhomNhanVien = true: chỉ nhận tài khoản nhân viên / quản trị; false: chỉ nhận khách hàng.
     */
    public TaiKhoan dangNhap(String tenHoacSdt, String matKhau, boolean nhomNhanVien) {
        String dieuKienVaiTro = nhomNhanVien ? "vai_tro <> " + TaiKhoan.KHACH_HANG
                : "vai_tro = " + TaiKhoan.KHACH_HANG;
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM tai_khoan WHERE (ten_dang_nhap = ? OR so_dien_thoai = ?)" +
                        " AND mat_khau = ? AND " + dieuKienVaiTro,
                new String[]{tenHoacSdt, tenHoacSdt, PhienDangNhap.bamMatKhau(matKhau)});
        TaiKhoan tk = c.moveToFirst() ? docTaiKhoan(c) : null;
        c.close();
        return tk;
    }

    public TaiKhoan layTaiKhoan(int id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM tai_khoan WHERE id = ?",
                new String[]{String.valueOf(id)});
        TaiKhoan tk = c.moveToFirst() ? docTaiKhoan(c) : null;
        c.close();
        return tk;
    }

    /** Kiểm tra trùng tên đăng nhập / số điện thoại (bỏ qua tài khoản boQuaId). */
    public boolean daTonTai(String tenDangNhap, String soDienThoai, int boQuaId) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM tai_khoan WHERE id <> ? AND (ten_dang_nhap = ? OR ten_dang_nhap = ?" +
                        " OR (so_dien_thoai <> '' AND so_dien_thoai = ?))",
                new String[]{String.valueOf(boQuaId), tenDangNhap, soDienThoai, soDienThoai});
        boolean co = c.moveToFirst() && c.getInt(0) > 0;
        c.close();
        return co;
    }

    private ContentValues giaTriTaiKhoan(TaiKhoan tk) {
        ContentValues cv = new ContentValues();
        cv.put("ten_dang_nhap", tk.getTenDangNhap());
        cv.put("ho_ten", tk.getHoTen());
        cv.put("so_dien_thoai", tk.getSoDienThoai());
        cv.put("email", tk.getEmail());
        cv.put("vai_tro", tk.getVaiTro());
        cv.put("chuc_vu", tk.getChucVu());
        cv.put("dang_hoat_dong", tk.isDangHoatDong() ? 1 : 0);
        return cv;
    }

    public int themTaiKhoan(TaiKhoan tk, String matKhau) {
        ContentValues cv = giaTriTaiKhoan(tk);
        cv.put("mat_khau", PhienDangNhap.bamMatKhau(matKhau));
        cv.put("ngay_tao", System.currentTimeMillis());
        return (int) getWritableDatabase().insert("tai_khoan", null, cv);
    }

    public void suaTaiKhoan(TaiKhoan tk) {
        getWritableDatabase().update("tai_khoan", giaTriTaiKhoan(tk), "id=?",
                new String[]{String.valueOf(tk.getId())});
    }

    public void datMatKhau(int taiKhoanId, String matKhauMoi) {
        ContentValues cv = new ContentValues();
        cv.put("mat_khau", PhienDangNhap.bamMatKhau(matKhauMoi));
        getWritableDatabase().update("tai_khoan", cv, "id=?", new String[]{String.valueOf(taiKhoanId)});
    }

    public boolean kiemTraMatKhau(int taiKhoanId, String matKhau) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM tai_khoan WHERE id = ? AND mat_khau = ?",
                new String[]{String.valueOf(taiKhoanId), PhienDangNhap.bamMatKhau(matKhau)});
        boolean dung = c.moveToFirst() && c.getInt(0) > 0;
        c.close();
        return dung;
    }

    public void xoaTaiKhoan(int id) {
        getWritableDatabase().delete("tai_khoan", "id=?", new String[]{String.valueOf(id)});
    }

    /** Nhân viên và quản trị viên, quản trị xếp trước. */
    public ArrayList<TaiKhoan> layDanhSachNhanVien() {
        ArrayList<TaiKhoan> ds = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM tai_khoan WHERE vai_tro <> ? ORDER BY vai_tro DESC, dang_hoat_dong DESC, ho_ten",
                new String[]{String.valueOf(TaiKhoan.KHACH_HANG)});
        while (c.moveToNext()) ds.add(docTaiKhoan(c));
        c.close();
        return ds;
    }

    public int demQuanTriDangHoatDong() {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM tai_khoan WHERE vai_tro = ? AND dang_hoat_dong = 1",
                new String[]{String.valueOf(TaiKhoan.QUAN_TRI)});
        int dem = c.moveToFirst() ? c.getInt(0) : 0;
        c.close();
        return dem;
    }

    // ===================== ĐẶT BÀN =====================

    private static final String SQL_DAT_BAN = "SELECT d.id, d.ma_dat, d.khach_id, t.ho_ten, t.so_dien_thoai," +
            " d.ban_id, d.ten_ban, d.khu_vuc, d.so_khach, d.thoi_gian, d.ghi_chu, d.trang_thai," +
            " d.ngay_tao, d.hoa_don_id FROM dat_ban d LEFT JOIN tai_khoan t ON t.id = d.khach_id";

    private ArrayList<DatBan> truyVanDatBan(String where, String[] args, String orderBy) {
        capNhatDatBanQuaHan();
        ArrayList<DatBan> ds = new ArrayList<>();
        String sql = SQL_DAT_BAN;
        if (where != null) sql += " WHERE " + where;
        sql += " ORDER BY " + orderBy;
        Cursor c = getReadableDatabase().rawQuery(sql, args);
        while (c.moveToNext()) {
            DatBan d = new DatBan();
            d.setId(c.getInt(0));
            d.setMaDat(c.getString(1));
            d.setKhachId(c.getInt(2));
            d.setTenKhach(c.getString(3));
            d.setSoDienThoai(c.getString(4));
            d.setBanId(c.getInt(5));
            d.setTenBan(c.getString(6));
            d.setKhuVuc(c.getString(7));
            d.setSoKhach(c.getInt(8));
            d.setThoiGian(c.getLong(9));
            d.setGhiChu(c.getString(10));
            d.setTrangThai(c.getInt(11));
            d.setNgayTao(c.getLong(12));
            d.setHoaDonId(c.getInt(13));
            ds.add(d);
        }
        c.close();
        return ds;
    }

    /** Lượt đặt quá giờ hẹn + thời gian chờ mà khách chưa đến -> "Không đến". */
    private void capNhatDatBanQuaHan() {
        ContentValues cv = new ContentValues();
        cv.put("trang_thai", DatBan.KHONG_DEN);
        getWritableDatabase().update("dat_ban", cv, "trang_thai = ? AND thoi_gian < ?",
                new String[]{String.valueOf(DatBan.CHO_DEN),
                        String.valueOf(System.currentTimeMillis() - DatBan.THOI_GIAN_CHO)});
    }

    public DatBan layDatBan(int id) {
        ArrayList<DatBan> ds = truyVanDatBan("d.id = ?", new String[]{String.valueOf(id)}, "d.id");
        return ds.isEmpty() ? null : ds.get(0);
    }

    public ArrayList<DatBan> layDatBanCuaKhach(int khachId) {
        return truyVanDatBan("d.khach_id = ?", new String[]{String.valueOf(khachId)},
                "d.trang_thai = 0 DESC, d.thoi_gian DESC");
    }

    /** Lượt đặt sắp tới (chờ đến) của khách, gần nhất trước. */
    public ArrayList<DatBan> layDatBanSapToi(int khachId) {
        return truyVanDatBan("d.khach_id = ? AND d.trang_thai = ?",
                new String[]{String.valueOf(khachId), String.valueOf(DatBan.CHO_DEN)}, "d.thoi_gian");
    }

    /** trangThai = -1 để lấy tất cả; tuKhoa tìm theo mã đặt, tên hoặc SĐT khách. */
    public ArrayList<DatBan> layDanhSachDatBan(int trangThai, long tuNgay, long denNgay, String tuKhoa) {
        StringBuilder where = new StringBuilder("1=1");
        ArrayList<String> args = new ArrayList<>();
        if (trangThai >= 0) {
            where.append(" AND d.trang_thai = ?");
            args.add(String.valueOf(trangThai));
        }
        if (tuNgay > 0) {
            where.append(" AND d.thoi_gian >= ? AND d.thoi_gian < ?");
            args.add(String.valueOf(tuNgay));
            args.add(String.valueOf(denNgay));
        }
        if (tuKhoa != null && !tuKhoa.trim().isEmpty()) {
            String k = "%" + tuKhoa.trim() + "%";
            where.append(" AND (d.ma_dat LIKE ? OR t.ho_ten LIKE ? OR t.so_dien_thoai LIKE ?)");
            args.add(k);
            args.add(k);
            args.add(k);
        }
        return truyVanDatBan(where.toString(), args.toArray(new String[0]),
                "d.trang_thai = 0 DESC, d.thoi_gian");
    }

    /**
     * Bàn còn trống tại thời điểm hẹn: đủ chỗ, không trùng lượt đặt khác trong khung 2 tiếng,
     * và nếu giờ hẹn nằm trong 2 tiếng tới thì bàn phải đang trống.
     */
    public ArrayList<Ban> layBanTrongLuc(long thoiGian, int soKhach, String khuVuc) {
        capNhatDatBanQuaHan();
        ArrayList<Ban> ds = new ArrayList<>();
        boolean ganHienTai = thoiGian - System.currentTimeMillis() < DatBan.THOI_LUONG_GIU_BAN;
        StringBuilder sql = new StringBuilder("SELECT id, ten, khu_vuc, so_cho, trang_thai FROM ban b" +
                " WHERE so_cho >= ? AND NOT EXISTS (SELECT 1 FROM dat_ban d WHERE d.ban_id = b.id" +
                " AND d.trang_thai = ? AND ABS(d.thoi_gian - CAST(? AS INTEGER)) < CAST(? AS INTEGER))");
        ArrayList<String> args = new ArrayList<>();
        args.add(String.valueOf(soKhach));
        args.add(String.valueOf(DatBan.CHO_DEN));
        args.add(String.valueOf(thoiGian));
        args.add(String.valueOf(DatBan.THOI_LUONG_GIU_BAN));
        if (ganHienTai) {
            sql.append(" AND trang_thai = ").append(Ban.TRONG);
        }
        if (khuVuc != null) {
            sql.append(" AND khu_vuc = ?");
            args.add(khuVuc);
        }
        sql.append(" ORDER BY so_cho, id");
        Cursor c = getReadableDatabase().rawQuery(sql.toString(), args.toArray(new String[0]));
        while (c.moveToNext()) {
            ds.add(new Ban(c.getInt(0), c.getString(1), c.getString(2), c.getInt(3), c.getInt(4)));
        }
        c.close();
        return ds;
    }

    /** Tạo lượt đặt, trả về null nếu bàn vừa bị người khác đặt mất. */
    public DatBan taoDatBan(int khachId, Ban ban, int soKhach, long thoiGian, String ghiChu) {
        boolean conTrong = false;
        for (Ban b : layBanTrongLuc(thoiGian, soKhach, ban.getKhuVuc())) {
            if (b.getId() == ban.getId()) conTrong = true;
        }
        if (!conTrong) return null;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("khach_id", khachId);
        cv.put("ban_id", ban.getId());
        cv.put("ten_ban", ban.getTenBan());
        cv.put("khu_vuc", ban.getKhuVuc());
        cv.put("so_khach", soKhach);
        cv.put("thoi_gian", thoiGian);
        cv.put("ghi_chu", ghiChu);
        cv.put("trang_thai", DatBan.CHO_DEN);
        cv.put("ngay_tao", System.currentTimeMillis());
        int id = (int) db.insert("dat_ban", null, cv);
        ganMa(db, "dat_ban", "ma_dat", "DB", id);
        return layDatBan(id);
    }

    public void capNhatTrangThaiDatBan(int datBanId, int trangThai) {
        ContentValues cv = new ContentValues();
        cv.put("trang_thai", trangThai);
        getWritableDatabase().update("dat_ban", cv, "id=?", new String[]{String.valueOf(datBanId)});
    }

    /**
     * Khách đến quán: mở bàn đã đặt, trả về id hóa đơn; trả về 0 nếu bàn đang có khách khác.
     */
    public int nhanBan(DatBan dat) {
        Ban ban = null;
        for (Ban b : layDanhSachBan(null)) {
            if (b.getId() == dat.getBanId()) ban = b;
        }
        if (ban == null || !ban.isTrong()) return 0;
        int hoaDonId = moBan(ban, dat.getSoKhach());
        ContentValues cv = new ContentValues();
        cv.put("trang_thai", DatBan.DA_NHAN_BAN);
        cv.put("hoa_don_id", hoaDonId);
        getWritableDatabase().update("dat_ban", cv, "id=?", new String[]{String.valueOf(dat.getId())});
        return hoaDonId;
    }

    /** Lượt đặt chờ đến gần nhất trong hôm nay của từng bàn (khóa = id bàn). */
    public HashMap<Integer, DatBan> layDatBanHomNayTheoBan() {
        HashMap<Integer, DatBan> kq = new HashMap<>();
        long now = System.currentTimeMillis();
        Calendar cuoiNgay = Calendar.getInstance();
        cuoiNgay.set(Calendar.HOUR_OF_DAY, 23);
        cuoiNgay.set(Calendar.MINUTE, 59);
        for (DatBan d : truyVanDatBan("d.trang_thai = ? AND d.thoi_gian >= ? AND d.thoi_gian <= ?",
                new String[]{String.valueOf(DatBan.CHO_DEN), String.valueOf(now - DatBan.THOI_GIAN_CHO),
                        String.valueOf(cuoiNgay.getTimeInMillis())}, "d.thoi_gian DESC")) {
            kq.put(d.getBanId(), d);
        }
        return kq;
    }

    // ===================== ĐƠN GỌI MÓN QUA APP =====================

    private static final String SQL_DON_ONLINE = "SELECT o.id, o.ma_don, o.khach_id, t.ho_ten, t.so_dien_thoai," +
            " o.dat_ban_id, d.ma_dat, d.ten_ban, d.khu_vuc, d.thoi_gian, o.hinh_thuc, o.phuong_thuc," +
            " o.da_thanh_toan, o.trang_thai, o.tong_tien, o.ghi_chu, o.thoi_gian," +
            " (SELECT IFNULL(SUM(so_luong),0) FROM don_online_ct WHERE don_id = o.id)" +
            " FROM don_online o LEFT JOIN tai_khoan t ON t.id = o.khach_id" +
            " LEFT JOIN dat_ban d ON d.id = o.dat_ban_id";

    private ArrayList<DonOnline> truyVanDonOnline(String where, String[] args) {
        ArrayList<DonOnline> ds = new ArrayList<>();
        String sql = SQL_DON_ONLINE;
        if (where != null) sql += " WHERE " + where;
        sql += " ORDER BY o.trang_thai IN (0,1) DESC, o.thoi_gian DESC";
        Cursor c = getReadableDatabase().rawQuery(sql, args);
        while (c.moveToNext()) {
            DonOnline o = new DonOnline();
            o.setId(c.getInt(0));
            o.setMaDon(c.getString(1));
            o.setKhachId(c.getInt(2));
            o.setTenKhach(c.getString(3));
            o.setSoDienThoai(c.getString(4));
            o.setDatBanId(c.getInt(5));
            if (!c.isNull(6)) {
                o.setThongTinBan(c.getString(7) + " • " + c.getString(8) + " • "
                        + TienIch.ngayGio(c.getLong(9)));
            }
            o.setHinhThuc(c.getString(10));
            o.setPhuongThuc(c.getString(11));
            o.setDaThanhToan(c.getInt(12) == 1);
            o.setTrangThai(c.getInt(13));
            o.setTongTien(c.getLong(14));
            o.setGhiChu(c.getString(15));
            o.setThoiGian(c.getLong(16));
            o.setSoMon(c.getInt(17));
            ds.add(o);
        }
        c.close();
        return ds;
    }

    /** khachId = 0 để lấy đơn của mọi khách; trangThai = -1 để lấy tất cả. */
    public ArrayList<DonOnline> layDanhSachDonOnline(int khachId, int trangThai) {
        StringBuilder where = new StringBuilder("1=1");
        ArrayList<String> args = new ArrayList<>();
        if (khachId > 0) {
            where.append(" AND o.khach_id = ?");
            args.add(String.valueOf(khachId));
        }
        if (trangThai >= 0) {
            where.append(" AND o.trang_thai = ?");
            args.add(String.valueOf(trangThai));
        }
        return truyVanDonOnline(where.toString(), args.toArray(new String[0]));
    }

    public DonOnline layDonOnline(int id) {
        ArrayList<DonOnline> ds = truyVanDonOnline("o.id = ?", new String[]{String.valueOf(id)});
        return ds.isEmpty() ? null : ds.get(0);
    }

    public ArrayList<DonOnline> layDonOnlineTheoDatBan(int datBanId) {
        return truyVanDonOnline("o.dat_ban_id = ?", new String[]{String.valueOf(datBanId)});
    }

    public ArrayList<ChiTietHoaDon> layChiTietDonOnline(int donId) {
        ArrayList<ChiTietHoaDon> ds = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT ct.id, ct.thuc_uong_id, ct.ten_mon, ct.don_gia, ct.so_luong, ct.ghi_chu," +
                        " t.danh_muc, t.hinh FROM don_online_ct ct" +
                        " LEFT JOIN thuc_uong t ON t.id = ct.thuc_uong_id WHERE ct.don_id = ?",
                new String[]{String.valueOf(donId)});
        while (c.moveToNext()) {
            ChiTietHoaDon ct = new ChiTietHoaDon();
            ct.setId(c.getInt(0));
            ct.setHoaDonId(donId);
            ct.setThucUongId(c.getInt(1));
            ct.setTenMon(c.getString(2));
            ct.setDonGia(c.getLong(3));
            ct.setSoLuong(c.getInt(4));
            ct.setGhiChu(c.getString(5));
            ct.setDanhMuc(c.getString(6));
            ct.setUriAnh(c.getString(7));
            ds.add(ct);
        }
        c.close();
        return ds;
    }

    /** Lưu đơn khách vừa đặt, trả về mã đơn. */
    public String taoDonOnline(DonOnline don, ArrayList<ChiTietHoaDon> dsMon) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long tong = 0;
            for (ChiTietHoaDon ct : dsMon) tong += ct.getThanhTien();
            ContentValues cv = new ContentValues();
            cv.put("khach_id", don.getKhachId());
            cv.put("dat_ban_id", don.getDatBanId());
            cv.put("hinh_thuc", don.getHinhThuc());
            cv.put("phuong_thuc", don.getPhuongThuc());
            cv.put("da_thanh_toan", don.isDaThanhToan() ? 1 : 0);
            cv.put("trang_thai", DonOnline.CHO_XAC_NHAN);
            cv.put("tong_tien", tong);
            cv.put("ghi_chu", don.getGhiChu());
            cv.put("thoi_gian", System.currentTimeMillis());
            int id = (int) db.insert("don_online", null, cv);
            String ma = ganMa(db, "don_online", "ma_don", "DO", id);
            for (ChiTietHoaDon ct : dsMon) {
                themDongDon(db, id, ct.getThucUongId(), ct.getTenMon(), ct.getDonGia(),
                        ct.getSoLuong(), ct.getGhiChu());
            }
            db.setTransactionSuccessful();
            return ma;
        } finally {
            db.endTransaction();
        }
    }

    public void capNhatTrangThaiDon(int donId, int trangThai) {
        ContentValues cv = new ContentValues();
        cv.put("trang_thai", trangThai);
        getWritableDatabase().update("don_online", cv, "id=?", new String[]{String.valueOf(donId)});
    }

    public void danhDauDaThanhToan(int donId, String phuongThuc) {
        ContentValues cv = new ContentValues();
        cv.put("da_thanh_toan", 1);
        cv.put("phuong_thuc", phuongThuc);
        getWritableDatabase().update("don_online", cv, "id=?", new String[]{String.valueOf(donId)});
    }

    /** Doanh thu đơn app đã thanh toán, không hủy, trong khoảng thời gian. {số đơn, tổng tiền} */
    public long[] doanhThuDonOnline(long tuNgay, long denNgay) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*), IFNULL(SUM(tong_tien),0) FROM don_online" +
                        " WHERE da_thanh_toan = 1 AND trang_thai <> ? AND thoi_gian >= ? AND thoi_gian < ?",
                new String[]{String.valueOf(DonOnline.DA_HUY), String.valueOf(tuNgay), String.valueOf(denNgay)});
        long[] kq = c.moveToFirst() ? new long[]{c.getLong(0), c.getLong(1)} : new long[]{0, 0};
        c.close();
        return kq;
    }
}
