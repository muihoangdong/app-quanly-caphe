package hutech.cntt.app_quanlycafe.data;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hutech.cntt.app_quanlycafe.data.ApiClient.ApiResponse;
import hutech.cntt.app_quanlycafe.data.ApiClient.LoiCSDL;
import hutech.cntt.app_quanlycafe.model.Ban;
import hutech.cntt.app_quanlycafe.model.ChiTietHoaDon;
import hutech.cntt.app_quanlycafe.model.DanhMuc;
import hutech.cntt.app_quanlycafe.model.HoaDon;
import hutech.cntt.app_quanlycafe.model.NhanVien;
import hutech.cntt.app_quanlycafe.model.ThucUong;
import retrofit2.Response;

/**
 * Lớp dữ liệu của app: gọi API PHP (server/api) qua Retrofit, đổi JSON thành model.
 * Các hàm chạy đồng bộ (execute) nên phải gọi trong ChayNen, không gọi trên luồng giao diện.
 */
public class CafeDatabase {

    private static final String TEN_FILE = "phien_lam_viec";
    private static final String KHOA_NHAN_VIEN = "nhan_vien_id";
    private static final String KHOA_QUAN_LY = "la_quan_ly";
    private static final String KHOA_TOKEN = "token";

    public static final String CD_TEN_QUAN = "shop_name";
    public static final String CD_TAI_KHOAN = "bank_account";

    private static CafeDatabase instance;
    private final ApiClient ketNoi;
    private final Context context;
    private final Gson gson = new Gson();

    public static synchronized CafeDatabase getInstance(Context context) {
        if (instance == null) instance = new CafeDatabase(context);
        return instance;
    }

    private CafeDatabase(Context context) {
        this.context = context.getApplicationContext();
        ketNoi = new ApiClient(this.context);
    }

    public ApiClient getKetNoi() {
        return ketNoi;
    }

    // ===================== GỌI API =====================

    /** Tạo body JSON từ các cặp khóa, giá trị. */
    private JsonObject thamSo(Object... cap) {
        JsonObject o = new JsonObject();
        for (int i = 0; i + 1 < cap.length; i += 2) o.add((String) cap[i], gson.toJsonTree(cap[i + 1]));
        return o;
    }

    /** Gọi một action của API, trả về phần "data". Lỗi mạng / lỗi nghiệp vụ đổi thành LoiCSDL. */
    private JsonElement goi(String tep, String action, Object... cap) throws LoiCSDL {
        JsonObject body = thamSo(cap);
        body.addProperty("action", action);
        Response<ApiResponse> phanHoi;
        try {
            phanHoi = ketNoi.getApiService().goi(tep, phien().getString(KHOA_TOKEN, ""), body).execute();
        } catch (IOException e) {
            Log.w("CafeDatabase", "Không gọi được " + tep + " " + action, e);
            throw new LoiCSDL("Không kết nối được máy chủ " + ketNoi.layDiaChi()
                    + ".\nKiểm tra Docker (MySQL + API) đã chạy và địa chỉ máy chủ còn đúng.");
        } catch (RuntimeException e) {
            Log.w("CafeDatabase", "Dữ liệu lỗi từ " + tep + " " + action, e);
            throw new LoiCSDL("Máy chủ trả dữ liệu không hợp lệ");
        }
        ApiResponse kq = phanHoi.body();
        if (phanHoi.isSuccessful() && kq != null && kq.isSuccess()) return kq.getData();
        throw new LoiCSDL(thongBaoLoi(phanHoi), phanHoi.code());
    }

    /** Lấy "message" trong JSON lỗi của API; không đọc được thì báo mã HTTP. */
    private static String thongBaoLoi(Response<ApiResponse> phanHoi) {
        if (phanHoi.body() != null && phanHoi.body().getMessage() != null) return phanHoi.body().getMessage();
        try {
            if (phanHoi.errorBody() != null) {
                JsonObject loi = JsonParser.parseString(phanHoi.errorBody().string()).getAsJsonObject();
                if (loi.has("message")) return loi.get("message").getAsString();
            }
        } catch (IOException | RuntimeException e) {
            Log.w("CafeDatabase", "Không đọc được nội dung lỗi", e);
        }
        return "Lỗi máy chủ (HTTP " + phanHoi.code() + ")";
    }

    private static ArrayList<JsonObject> cacDong(JsonElement data) {
        ArrayList<JsonObject> ds = new ArrayList<>();
        if (data != null && data.isJsonArray()) {
            for (JsonElement e : data.getAsJsonArray()) ds.add(e.getAsJsonObject());
        }
        return ds;
    }

    // ----- Đọc giá trị trong JSON (cột NULL thì trả 0 / null) -----

    private static boolean coGiaTri(JsonObject o, String khoa) {
        return o.has(khoa) && !o.get(khoa).isJsonNull();
    }

    private static String chuoi(JsonObject o, String khoa) {
        return coGiaTri(o, khoa) ? o.get(khoa).getAsString() : null;
    }

    /** SUM / UNIX_TIMESTAMP có thể về dạng chuỗi số, nên đọc qua BigDecimal. */
    private static long so(JsonObject o, String khoa) {
        return coGiaTri(o, khoa) ? new BigDecimal(o.get(khoa).getAsString()).longValue() : 0;
    }

    private static int nguyen(JsonObject o, String khoa) {
        return (int) so(o, khoa);
    }

    private static boolean dung(JsonObject o, String khoa) {
        return so(o, khoa) != 0;
    }

    public void kiemTraKetNoi() throws LoiCSDL {
        goi("cauhinh.php", "kiemTra");
    }

    //CẤU HÌNH QUÁN (Settings)

    public LinkedHashMap<String, String> layCauHinh() throws LoiCSDL {
        LinkedHashMap<String, String> kq = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : goi("cauhinh.php", "lay").getAsJsonObject().entrySet()) {
            kq.put(e.getKey(), e.getValue().isJsonNull() ? "" : e.getValue().getAsString());
        }
        return kq;
    }

    //NHÂN VIÊN (Users)

    public static final String MAT_KHAU_MAC_DINH = "123456";

    private static NhanVien docNhanVien(JsonObject o) {
        return new NhanVien(nguyen(o, "id"), chuoi(o, "tenDangNhap"), chuoi(o, "hoTen"),
                dung(o, "quanLy"), dung(o, "hoatDong"));
    }

    private SharedPreferences phien() {
        return context.getSharedPreferences(TEN_FILE, Context.MODE_PRIVATE);
    }

    private void luuPhien(NhanVien nv) {
        phien().edit().putInt(KHOA_NHAN_VIEN, nv.getId()).putBoolean(KHOA_QUAN_LY, nv.isQuanLy()).apply();
    }

    /** Phiên cũ (bản kết nối thẳng MySQL) không có token thì coi như chưa đăng nhập. */
    public boolean daDangNhap() {
        return idNguoiDangNhap() > 0 && !phien().getString(KHOA_TOKEN, "").isEmpty();
    }

    public int idNguoiDangNhap() {
        return phien().getInt(KHOA_NHAN_VIEN, 0);
    }

    public boolean laQuanLy() {
        return daDangNhap() && phien().getBoolean(KHOA_QUAN_LY, false);
    }

    public NhanVien dangNhap(String tenDangNhap, String matKhau) throws LoiCSDL {
        JsonObject kq = goi("taikhoan.php", "dangNhap", "tenDangNhap", tenDangNhap.trim(), "matKhau", matKhau)
                .getAsJsonObject();
        NhanVien nv = docNhanVien(kq.getAsJsonObject("nhanVien"));
        phien().edit().putString(KHOA_TOKEN, kq.get("token").getAsString()).apply();
        luuPhien(nv);
        return nv;
    }

    public void dangXuat() {
        phien().edit().remove(KHOA_NHAN_VIEN).remove(KHOA_QUAN_LY).remove(KHOA_TOKEN).apply();
    }

    /** Người đang đăng nhập; null nếu chưa đăng nhập, token hết hạn hoặc tài khoản bị khóa. */
    public NhanVien layNguoiDangNhap() throws LoiCSDL {
        if (!daDangNhap()) return null;
        try {
            NhanVien nv = docNhanVien(goi("taikhoan.php", "nguoiDangNhap").getAsJsonObject());
            luuPhien(nv);
            return nv;
        } catch (LoiCSDL e) {
            if (e.isHetPhien()) return null;
            throw e;
        }
    }

    public void doiMatKhau(String matKhauCu, String matKhauMoi) throws LoiCSDL {
        goi("taikhoan.php", "doiMatKhau", "matKhauCu", matKhauCu, "matKhauMoi", matKhauMoi);
    }

    //Quản lý tài khoản (chỉ quản lý, API kiểm tra lại quyền)

    public ArrayList<NhanVien> layTatCaNhanVien() throws LoiCSDL {
        ArrayList<NhanVien> ds = new ArrayList<>();
        for (JsonObject o : cacDong(goi("taikhoan.php", "danhSach"))) ds.add(docNhanVien(o));
        return ds;
    }

    public int themNhanVien(String tenDangNhap, String hoTen, boolean quanLy, String soDienThoai) throws LoiCSDL {
        return goi("taikhoan.php", "them", "tenDangNhap", tenDangNhap.trim(), "hoTen", hoTen.trim(),
                "quanLy", quanLy, "soDienThoai", soDienThoai).getAsInt();
    }

    public void datLaiMatKhau(int id) throws LoiCSDL {
        goi("taikhoan.php", "datLaiMatKhau", "id", id);
    }

    public void khoaTaiKhoan(int id, boolean khoa) throws LoiCSDL {
        goi("taikhoan.php", "khoa", "id", id, "khoa", khoa);
    }

    //BÀN

    private static Ban docBan(JsonObject o) {
        Ban ban = new Ban(nguyen(o, "id"), chuoi(o, "tenBan"), chuoi(o, "khuVuc"), nguyen(o, "soCho"),
                Ban.tuMa(chuoi(o, "trangThai")));
        if (coGiaTri(o, "hoaDonId")) {
            ban.setHoaDonId(nguyen(o, "hoaDonId"));
            ban.setSoKhach(nguyen(o, "soKhach"));
            ban.setGioVao(so(o, "gioVao"));
            ban.setSoMon(nguyen(o, "soMon"));
            ban.setTamTinh(so(o, "tamTinh"));
        }
        return ban;
    }

    public ArrayList<Ban> layDanhSachBan() throws LoiCSDL {
        ArrayList<Ban> ds = new ArrayList<>();
        for (JsonObject o : cacDong(goi("ban.php", "danhSach"))) ds.add(docBan(o));
        return ds;
    }

    public void themBan(Ban ban) throws LoiCSDL {
        goi("ban.php", "them", "tenBan", ban.getTenBan(), "khuVuc", ban.getKhuVuc(), "soCho", ban.getSoCho());
    }

    public void suaBan(Ban ban) throws LoiCSDL {
        goi("ban.php", "sua", "id", ban.getId(), "tenBan", ban.getTenBan(), "khuVuc", ban.getKhuVuc(),
                "soCho", ban.getSoCho());
    }

    public void xoaBan(int banId) throws LoiCSDL {
        goi("ban.php", "xoa", "id", banId);
    }

    public void capNhatTrangThaiBan(int banId, int trangThai) throws LoiCSDL {
        goi("ban.php", "capNhatTrangThai", "id", banId, "trangThai", Ban.maTrangThai(trangThai));
    }

    public void doiBan(int hoaDonId, int banMoiId) throws LoiCSDL {
        goi("ban.php", "doiBan", "hoaDonId", hoaDonId, "banMoiId", banMoiId);
    }

    public void gopBan(int hoaDonNguonId, int hoaDonDichId) throws LoiCSDL {
        if (hoaDonNguonId == hoaDonDichId) throw new LoiCSDL("Không thể gộp vào chính nó");
        goi("ban.php", "gopBan", "hoaDonNguonId", hoaDonNguonId, "hoaDonDichId", hoaDonDichId);
    }

    //DANH MỤC

    public ArrayList<DanhMuc> layDanhSachDanhMuc() throws LoiCSDL {
        ArrayList<DanhMuc> ds = new ArrayList<>();
        for (JsonObject o : cacDong(goi("thucdon.php", "danhMuc"))) {
            ds.add(new DanhMuc(nguyen(o, "soMon"), chuoi(o, "ten"), nguyen(o, "id")));
        }
        return ds;
    }

    public DanhMuc themDanhMuc(String ten) throws LoiCSDL {
        return new DanhMuc(0, ten, goi("thucdon.php", "themDanhMuc", "ten", ten).getAsInt());
    }

    public void doiTenDanhMuc(int id, String tenMoi) throws LoiCSDL {
        goi("thucdon.php", "doiTenDanhMuc", "id", id, "ten", tenMoi);
    }

    public void xoaDanhMuc(int id) throws LoiCSDL {
        goi("thucdon.php", "xoaDanhMuc", "id", id);
    }

    //THỨC UỐNG

    private static ThucUong docThucUong(JsonObject o) {
        ThucUong tu = new ThucUong(nguyen(o, "id"), chuoi(o, "maMon"), chuoi(o, "tenMon"), chuoi(o, "danhMuc"),
                chuoi(o, "moTa"), so(o, "gia"), so(o, "giaVon"), chuoi(o, "uriAnh"),
                dung(o, "conBan"), dung(o, "banChay"));
        tu.setDanhMucId(nguyen(o, "danhMucId"));
        return tu;
    }

    /** Gồm cả món tạm hết (Gọi món hiện làm mờ để nhân viên báo khách). */
    public ArrayList<ThucUong> layDanhSachThucUong(int danhMucId, String tuKhoa) throws LoiCSDL {
        ArrayList<ThucUong> ds = new ArrayList<>();
        for (JsonObject o : cacDong(goi("thucdon.php", "danhSachMon", "danhMucId", danhMucId,
                "tuKhoa", tuKhoa == null ? "" : tuKhoa))) {
            ds.add(docThucUong(o));
        }
        return ds;
    }

    public ThucUong layThucUong(int id) throws LoiCSDL {
        return docThucUong(goi("thucdon.php", "layMon", "id", id).getAsJsonObject());
    }

    private Object[] giaTriThucUong(ThucUong tu) {
        return new Object[]{"id", tu.getId(), "danhMucId", tu.getDanhMucId(), "maMon", tu.getMaMon(),
                "tenMon", tu.getTenMon(), "moTa", tu.getMoTa(), "gia", tu.getGia(), "giaVon", tu.getGiaVon(),
                "uriAnh", tu.getUriAnh(), "conBan", tu.isConBan(), "banChay", tu.isBanChay()};
    }

    public void themThucUong(ThucUong tu) throws LoiCSDL {
        goi("thucdon.php", "themMon", giaTriThucUong(tu));
    }

    public void suaThucUong(ThucUong tu) throws LoiCSDL {
        goi("thucdon.php", "suaMon", giaTriThucUong(tu));
    }

    public void xoaThucUong(int id) throws LoiCSDL {
        goi("thucdon.php", "xoaMon", "id", id);
    }

    //ĐƠN GỌI MÓN / HÓA ĐƠN

    private static HoaDon docHoaDon(JsonObject o) {
        HoaDon hd = new HoaDon();
        hd.setId(nguyen(o, "id"));
        hd.setBanId(nguyen(o, "banId"));
        hd.setTenBan(chuoi(o, "tenBan"));
        hd.setKhuVuc(chuoi(o, "khuVuc"));
        hd.setSoKhach(nguyen(o, "soKhach"));
        String trangThai = chuoi(o, "trangThai");
        hd.setTrangThai("PAID".equals(trangThai) ? HoaDon.DA_THANH_TOAN
                : "CANCELLED".equals(trangThai) ? HoaDon.DA_HUY : HoaDon.DANG_PHUC_VU);
        hd.setGioVao(so(o, "gioVao"));
        hd.setSoMon(nguyen(o, "soMon"));
        hd.setTenNhanVien(chuoi(o, "tenNhanVien"));
        if (!coGiaTri(o, "hoaDonId")) {
            // Chưa có hóa đơn: đang phục vụ hoặc đã hủy
            hd.setMaHoaDon(String.format(Locale.ROOT, "DH%04d", hd.getId()));
            hd.setTamTinh(so(o, "tamTinhDon"));
            hd.setGioRa(so(o, "gioDong"));
        } else {
            hd.setMaHoaDon(chuoi(o, "maHoaDon"));
            hd.setTamTinh(so(o, "tamTinh"));
            hd.setGiamGia(so(o, "giamGia"));
            hd.setTongTien(so(o, "tongTien"));
            hd.setPhuongThuc(HoaDon.tenPhuongThuc(chuoi(o, "phuongThuc")));
            hd.setKhachDua(so(o, "khachDua"));
            hd.setGioRa(so(o, "gioThanhToan"));
        }
        return hd;
    }

    private static ArrayList<HoaDon> docDanhSachHoaDon(JsonElement data) {
        ArrayList<HoaDon> ds = new ArrayList<>();
        for (JsonObject o : cacDong(data)) ds.add(docHoaDon(o));
        return ds;
    }

    public HoaDon layHoaDon(int id) throws LoiCSDL {
        return docHoaDon(goi("donhang.php", "layDon", "id", id).getAsJsonObject());
    }

    /** Các đơn đang phục vụ (chưa thanh toán / chưa hủy), mới nhất lên đầu. */
    public ArrayList<HoaDon> layDonDangPhucVu() throws LoiCSDL {
        return docDanhSachHoaDon(goi("donhang.php", "dangPhucVu"));
    }

    public ArrayList<HoaDon> timHoaDonDaThanhToan(String tuKhoa, long tuNgay, long denNgay, int gioiHan)
            throws LoiCSDL {
        return docDanhSachHoaDon(goi("donhang.php", "timDaThanhToan", "tuKhoa", tuKhoa == null ? "" : tuKhoa,
                "tuNgay", tuNgay, "denNgay", denNgay, "gioiHan", gioiHan));
    }

    public ArrayList<ChiTietHoaDon> layChiTietHoaDon(int hoaDonId) throws LoiCSDL {
        ArrayList<ChiTietHoaDon> ds = new ArrayList<>();
        for (JsonObject o : cacDong(goi("donhang.php", "chiTiet", "hoaDonId", hoaDonId))) {
            ChiTietHoaDon ct = new ChiTietHoaDon();
            ct.setId(nguyen(o, "id"));
            ct.setHoaDonId(nguyen(o, "hoaDonId"));
            ct.setThucUongId(nguyen(o, "thucUongId"));
            ct.setTenMon(chuoi(o, "tenMon"));
            ct.setDonGia(so(o, "donGia"));
            ct.setSoLuong(nguyen(o, "soLuong"));
            ct.setGhiChu(chuoi(o, "ghiChu"));
            ct.setTrangThai("SERVED".equals(chuoi(o, "trangThai"))
                    ? ChiTietHoaDon.DA_LEN_MON : ChiTietHoaDon.DANG_PHA_CHE);
            ct.setThoiGian(so(o, "thoiGian"));
            ct.setDanhMuc(chuoi(o, "danhMuc"));
            ct.setUriAnh(chuoi(o, "uriAnh"));
            ds.add(ct);
        }
        return ds;
    }

    /** Danh sách món gửi lên API: thucUongId, soLuong, ghiChu. */
    private static JsonArray dsMonGui(ArrayList<ChiTietHoaDon> dsMon) {
        JsonArray mang = new JsonArray();
        for (ChiTietHoaDon ct : dsMon) {
            JsonObject o = new JsonObject();
            o.addProperty("thucUongId", ct.getThucUongId());
            o.addProperty("soLuong", ct.getSoLuong());
            o.addProperty("ghiChu", ct.getGhiChu() == null ? "" : ct.getGhiChu());
            mang.add(o);
        }
        return mang;
    }

    public int taoDonHang(int banId, int soKhach, String tenKhach, ArrayList<ChiTietHoaDon> dsMon) throws LoiCSDL {
        if (dsMon.isEmpty()) throw new LoiCSDL("Giỏ hàng đang trống");
        return goi("donhang.php", "taoDon", "banId", banId, "soKhach", soKhach,
                "tenKhach", tenKhach == null ? "" : tenKhach, "dsMon", dsMonGui(dsMon)).getAsInt();
    }

    public void themMonVaoHoaDon(int hoaDonId, ArrayList<ChiTietHoaDon> dsMon) throws LoiCSDL {
        goi("donhang.php", "themMon", "hoaDonId", hoaDonId, "dsMon", dsMonGui(dsMon));
    }

    public void capNhatSoLuong(int chiTietId, int soLuong) throws LoiCSDL {
        goi("donhang.php", "capNhatSoLuong", "chiTietId", chiTietId, "soLuong", soLuong);
    }

    public void capNhatTrangThaiMon(int chiTietId, int trangThai) throws LoiCSDL {
        goi("donhang.php", "capNhatTrangThaiMon", "chiTietId", chiTietId,
                "daLenMon", trangThai == ChiTietHoaDon.DA_LEN_MON);
    }

    public void xoaChiTiet(int chiTietId) throws LoiCSDL {
        capNhatSoLuong(chiTietId, 0);
    }

    public void yeuCauThanhToan(int hoaDonId) throws LoiCSDL {
        goi("donhang.php", "yeuCauThanhToan", "hoaDonId", hoaDonId);
    }

    public void huyHoaDon(int hoaDonId) throws LoiCSDL {
        goi("donhang.php", "huy", "hoaDonId", hoaDonId);
    }

    public HoaDon thanhToan(int hoaDonId, long giamGia, String phuongThuc, long khachDua) throws LoiCSDL {
        return docHoaDon(goi("donhang.php", "thanhToan", "hoaDonId", hoaDonId, "giamGia", giamGia,
                "phuongThuc", HoaDon.maPhuongThuc(phuongThuc), "khachDua", khachDua).getAsJsonObject());
    }

    // ===================== CHẠY NỀN =====================

    /**
     * Chạy việc gọi API trên luồng nền rồi trả kết quả / lỗi về luồng giao diện.
     * Bỏ qua kết quả nếu màn hình đã đóng.
     */
    public static class ChayNen {

        public interface Viec<T> {
            T lam() throws LoiCSDL;
        }

        public interface KetQua<T> {
            void nhan(T ketQua);
        }

        public interface Loi {
            void nhan(String thongBao);
        }

        private static final ExecutorService LUONG_NEN = Executors.newFixedThreadPool(2);
        private static final Handler LUONG_GIAO_DIEN = new Handler(Looper.getMainLooper());

        public static <T> void chay(Activity activity, Viec<T> viec, KetQua<T> ketQua) {
            chay(activity, viec, ketQua, thongBao -> Toast.makeText(activity, thongBao, Toast.LENGTH_LONG).show());
        }

        public static <T> void chay(Activity activity, Viec<T> viec, KetQua<T> ketQua, Loi loi) {
            LUONG_NEN.execute(() -> {
                T kq;
                try {
                    kq = viec.lam();
                } catch (LoiCSDL e) {
                    LUONG_GIAO_DIEN.post(() -> {
                        if (conSong(activity)) loi.nhan(e.getMessage());
                    });
                    return;
                }
                LUONG_GIAO_DIEN.post(() -> {
                    if (conSong(activity) && ketQua != null) ketQua.nhan(kq);
                });
            });
        }

        private static boolean conSong(Activity activity) {
            return !activity.isFinishing() && !activity.isDestroyed();
        }
    }
}
