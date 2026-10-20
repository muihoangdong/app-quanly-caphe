package hutech.cntt.app_quanlycafe.data;

import android.content.res.ColorStateList;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import hutech.cntt.app_quanlycafe.R;
import hutech.cntt.app_quanlycafe.model.ChiTietHoaDon;

/**
 * Các hàm tiện ích dùng chung: định dạng tiền, giờ và hiển thị ảnh món.
 */
public class TienIch {

    private static final DecimalFormat DINH_DANG_TIEN;

    static {
        DecimalFormatSymbols kyHieu = new DecimalFormatSymbols(Locale.getDefault());
        kyHieu.setGroupingSeparator('.');
        DINH_DANG_TIEN = new DecimalFormat("#,##0", kyHieu);
    }

    /** Làm tròn tới hàng nghìn đồng (giống cách máy chủ tính tổng hóa đơn). */
    public static long lamTron(long soTien) {
        return Math.round(soTien / 1000.0) * 1000;
    }

    /** 0:00 của ngày chứa thời điểm. */
    public static long dauNgay(long thoiGian) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(thoiGian);
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** 185000 -> "185.000đ" */
    public static String tien(long soTien) {
        return DINH_DANG_TIEN.format(soTien) + "đ";
    }

    /** 185000 -> "185.000" */
    public static String so(long soTien) {
        return DINH_DANG_TIEN.format(soTien);
    }

    /** Đọc số tiền người dùng nhập (bỏ dấu chấm, chữ đ...). */
    public static long docSo(String chuoi) {
        if (chuoi == null) return 0;
        String chiSo = chuoi.replaceAll("[^0-9]", "");
        if (chiSo.isEmpty()) return 0;
        try {
            return Long.parseLong(chiSo);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String gio(long thoiGian) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(thoiGian));
    }

    public static String ngay(long thoiGian) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(thoiGian));
    }

    public static String ngayGio(long thoiGian) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(thoiGian));
    }

    /** Thời gian đã ngồi: "45m" hoặc "1h10". */
    public static String thoiGianNgoi(long gioVao) {
        long phut = Math.max(0, (System.currentTimeMillis() - gioVao) / 60000);
        if (phut < 60) return phut + "m";
        return (phut / 60) + "h" + String.format(Locale.getDefault(), "%02d", phut % 60);
    }

    /** Đổ danh sách món vào một LinearLayout, mỗi món một dòng (item_dong_hoa_don). */
    public static void hienThiDongMon(LinearLayout container, ArrayList<ChiTietHoaDon> dsMon) {
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        for (ChiTietHoaDon ct : dsMon) {
            View dong = inflater.inflate(R.layout.item_dong_hoa_don, container, false);
            ((TextView) dong.findViewById(R.id.txtSoLuongDong)).setText(ct.getSoLuong() + "×");
            ((TextView) dong.findViewById(R.id.txtTenDong)).setText(ct.getTenMon());
            String ghiChu = tien(ct.getDonGia());
            if (ct.getGhiChu() != null && !ct.getGhiChu().isEmpty()) ghiChu += " • " + ct.getGhiChu();
            ((TextView) dong.findViewById(R.id.txtGhiChuDong)).setText(ghiChu);
            ((TextView) dong.findViewById(R.id.txtThanhTienDong)).setText(tien(ct.getThanhTien()));
            container.addView(dong);
        }
    }

    /** Biểu tượng mặc định theo danh mục khi món chưa có ảnh. */
    public static int bieuTuongDanhMuc(String danhMuc) {
        if (danhMuc == null) return R.drawable.ic_coffee;
        if (danhMuc.startsWith("Trà") || danhMuc.startsWith("Đá xay")) return R.drawable.ic_drink;
        if (danhMuc.startsWith("Bánh")) return R.drawable.ic_cake;
        return R.drawable.ic_coffee;
    }

    private static int mauNenDanhMuc(String danhMuc) {
        if (danhMuc == null) return R.color.caramel_soft;
        if (danhMuc.startsWith("Trà")) return R.color.status_amber_bg;
        if (danhMuc.startsWith("Đá xay")) return R.color.status_green_bg;
        if (danhMuc.startsWith("Bánh")) return R.color.latte_dark;
        if (danhMuc.contains("pha máy")) return R.color.latte;
        return R.color.caramel_soft;
    }

    /** Hiển thị ảnh món: ảnh người dùng chọn (uri) hoặc biểu tượng theo danh mục. */
    public static void hienThiAnhMon(ImageView img, String uriAnh, String danhMuc) {
        if (uriAnh != null && !uriAnh.isEmpty()) {
            try {
                img.setImageURI(Uri.parse(uriAnh));
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                img.setImageTintList(null);
                img.setBackgroundTintList(null);
                if (img.getDrawable() != null) return;
            } catch (Exception ignored) {
                // Ảnh đã bị xóa hoặc mất quyền đọc -> dùng biểu tượng mặc định
            }
        }
        img.setImageResource(bieuTuongDanhMuc(danhMuc));
        img.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        img.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(img.getContext(), R.color.mocha)));
        img.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(img.getContext(), mauNenDanhMuc(danhMuc))));
    }
}
