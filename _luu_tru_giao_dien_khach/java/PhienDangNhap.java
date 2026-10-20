package hutech.cntt.app_quanlycafe.data;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import hutech.cntt.app_quanlycafe.DangNhapActivity;
import hutech.cntt.app_quanlycafe.GiaoDienBanActivity;
import hutech.cntt.app_quanlycafe.KhachTrangChuActivity;
import hutech.cntt.app_quanlycafe.model.TaiKhoan;

/**
 * Lưu phiên đăng nhập (SharedPreferences) và băm mật khẩu.
 */
public class PhienDangNhap {

    private static final String TEN_FILE = "phien_dang_nhap";
    private static final String KHOA_ID = "tai_khoan_id";

    private static TaiKhoan taiKhoanHienTai;

    public static void luu(Context context, TaiKhoan tk) {
        taiKhoanHienTai = tk;
        context.getSharedPreferences(TEN_FILE, Context.MODE_PRIVATE)
                .edit().putInt(KHOA_ID, tk.getId()).apply();
    }

    /** Tài khoản đang đăng nhập, null nếu chưa đăng nhập hoặc tài khoản đã bị khóa. */
    public static TaiKhoan layTaiKhoan(Context context) {
        SharedPreferences sp = context.getSharedPreferences(TEN_FILE, Context.MODE_PRIVATE);
        int id = sp.getInt(KHOA_ID, 0);
        if (id == 0) return null;
        if (taiKhoanHienTai == null || taiKhoanHienTai.getId() != id) {
            taiKhoanHienTai = CafeDatabase.getInstance(context).layTaiKhoan(id);
        }
        if (taiKhoanHienTai == null || !taiKhoanHienTai.isDangHoatDong()) {
            dangXuatKhongChuyen(context);
            return null;
        }
        return taiKhoanHienTai;
    }

    /** Đọc lại tài khoản từ CSDL sau khi sửa thông tin. */
    public static void lamMoi(Context context) {
        taiKhoanHienTai = null;
        layTaiKhoan(context);
    }

    private static void dangXuatKhongChuyen(Context context) {
        taiKhoanHienTai = null;
        GioHangKhach.xoaHet();
        context.getSharedPreferences(TEN_FILE, Context.MODE_PRIVATE).edit().clear().apply();
    }

    public static void dangXuat(Activity activity) {
        dangXuatKhongChuyen(activity);
        Intent in = new Intent(activity, DangNhapActivity.class);
        in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(in);
        activity.finish();
    }

    /** Mở màn hình chính đúng với vai trò của tài khoản và xóa các màn hình cũ. */
    public static void moManHinhChinh(Activity activity, TaiKhoan tk) {
        Class<?> manHinh = tk.getVaiTro() == TaiKhoan.KHACH_HANG
                ? KhachTrangChuActivity.class : GiaoDienBanActivity.class;
        Intent in = new Intent(activity, manHinh);
        in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(in);
        activity.finish();
    }

    /**
     * Gọi đầu onCreate của màn hình cần đăng nhập. Trả về false (và đóng màn hình)
     * nếu chưa đăng nhập hoặc vai trò không được phép.
     */
    public static boolean kiemTraQuyen(Activity activity, boolean choKhach, boolean choNhanVien,
                                       boolean choQuanTri) {
        TaiKhoan tk = layTaiKhoan(activity);
        boolean duocPhep = tk != null && (
                (tk.getVaiTro() == TaiKhoan.KHACH_HANG && choKhach)
                        || (tk.getVaiTro() == TaiKhoan.NHAN_VIEN && choNhanVien)
                        || (tk.getVaiTro() == TaiKhoan.QUAN_TRI && choQuanTri));
        if (!duocPhep) {
            if (tk == null) {
                dangXuat(activity);
            } else {
                moManHinhChinh(activity, tk);
            }
        }
        return duocPhep;
    }

    /** Băm SHA-256 để không lưu mật khẩu dạng chữ thường trong CSDL. */
    public static String bamMatKhau(String matKhau) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bam = md.digest(matKhau.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bam) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
