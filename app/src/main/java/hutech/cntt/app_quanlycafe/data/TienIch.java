package hutech.cntt.app_quanlycafe.data;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import hutech.cntt.app_quanlycafe.R;
import hutech.cntt.app_quanlycafe.model.ChiTietHoaDon;


public class TienIch {

    private static final DecimalFormat DINH_DANG_TIEN;

    static {
        DecimalFormatSymbols kyHieu = new DecimalFormatSymbols(Locale.getDefault());
        kyHieu.setGroupingSeparator('.');
        DINH_DANG_TIEN = new DecimalFormat("#,##0", kyHieu);
    }


    public static long lamTron(long soTien) {
        return Math.round(soTien / 1000.0) * 1000;
    }


    public static long dauNgay(long thoiGian) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(thoiGian);
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }


    public static String tien(long soTien) {
        return DINH_DANG_TIEN.format(soTien) + "đ";
    }


    public static String so(long soTien) {
        return DINH_DANG_TIEN.format(soTien);
    }


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


    public static String thoiGianNgoi(long gioVao) {
        long phut = Math.max(0, (System.currentTimeMillis() - gioVao) / 60000);
        if (phut < 60) return phut + "m";
        return (phut / 60) + "h" + String.format(Locale.getDefault(), "%02d", phut % 60);
    }


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


    public static int anhMinhHoa(String tenMon, String danhMuc) {
        String ten = (tenMon == null ? "" : tenMon).toLowerCase(Locale.ROOT);
        String dm = (danhMuc == null ? "" : danhMuc).toLowerCase(Locale.ROOT);
        if (ten.contains("bánh") || dm.startsWith("bánh")) return R.drawable.img_mon_banh;
        if (ten.contains("matcha")) return R.drawable.img_mon_matcha;
        if (ten.contains("trà sữa") || dm.startsWith("trà sữa")) return R.drawable.img_mon_tra_sua;
        if (ten.contains("ép") || ten.contains("sinh tố") || dm.startsWith("nước")) return R.drawable.img_mon_nuoc_ep;
        if (ten.contains("trà") || dm.startsWith("trà")) return R.drawable.img_mon_tra;
        if (ten.contains("bạc xỉu")) return R.drawable.img_mon_bac_xiu;
        if (ten.contains("latte") || ten.contains("cappuccino")) return R.drawable.img_mon_latte;
        if (ten.contains("sữa")) return R.drawable.img_mon_ca_phe_sua;
        return R.drawable.img_mon_ca_phe_den;
    }

    public static void hienThiAnhMon(ImageView img, String uriAnh, String danhMuc) {
        hienThiAnhMon(img, uriAnh, null, danhMuc);
    }

    public static void hienThiAnhMon(ImageView img, String uriAnh, String tenMon, String danhMuc) {
        img.setImageTintList(null);
        img.setBackgroundTintList(null);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (uriAnh != null && uriAnh.startsWith("res:")) {
            int id = img.getResources().getIdentifier(uriAnh.substring(4), "drawable",
                    img.getContext().getPackageName());
            if (id != 0) {
                img.setImageResource(id);
                return;
            }
        } else if (uriAnh != null && !uriAnh.isEmpty()) {
            try {
                img.setImageURI(Uri.parse(uriAnh));
                if (img.getDrawable() != null) return;
            } catch (Exception ignored) {
                // Ảnh đã bị xóa khỏi máy: dùng ảnh minh họa
            }
        }
        img.setImageResource(anhMinhHoa(tenMon, danhMuc));
    }
}
