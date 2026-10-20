package hutech.cntt.app_quanlycafe.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import hutech.cntt.app_quanlycafe.model.ChiTietHoaDon;
import hutech.cntt.app_quanlycafe.model.ThucUong;

/**
 * Giỏ hàng của khách, dùng chung giữa màn hình Đặt món và Giỏ hàng (giữ trong bộ nhớ).
 */
public class GioHangKhach {

    // Khóa: thucUongId + "|" + ghiChu
    private static final LinkedHashMap<String, ChiTietHoaDon> DS_MON = new LinkedHashMap<>();

    /** Lượt đặt bàn khách vừa chọn "Gọi món trước" (0 = chưa chọn), giỏ hàng sẽ chọn sẵn. */
    public static int datBanIdGoiY = 0;

    public static void them(ThucUong tu, int soLuong, String ghiChu) {
        String gc = ghiChu == null ? "" : ghiChu.trim();
        String khoa = tu.getId() + "|" + gc;
        ChiTietHoaDon ct = DS_MON.get(khoa);
        if (ct == null) {
            ct = new ChiTietHoaDon();
            ct.setThucUongId(tu.getId());
            ct.setTenMon(tu.getTenMon());
            ct.setDanhMuc(tu.getDanhMuc());
            ct.setUriAnh(tu.getUriAnh());
            ct.setDonGia(tu.getGia());
            ct.setGhiChu(gc);
            ct.setSoLuong(0);
            DS_MON.put(khoa, ct);
        }
        ct.setSoLuong(ct.getSoLuong() + soLuong);
    }

    /** Đổi số lượng một dòng, về 0 thì bỏ khỏi giỏ. */
    public static void doiSoLuong(ChiTietHoaDon ct, int soLuongMoi) {
        if (soLuongMoi <= 0) {
            DS_MON.values().remove(ct);
        } else {
            ct.setSoLuong(soLuongMoi);
        }
    }

    public static ArrayList<ChiTietHoaDon> layDanhSach() {
        return new ArrayList<>(DS_MON.values());
    }

    public static int tongSoLuong() {
        int tong = 0;
        for (ChiTietHoaDon ct : DS_MON.values()) tong += ct.getSoLuong();
        return tong;
    }

    public static int soLuongCuaMon(int thucUongId) {
        int tong = 0;
        for (ChiTietHoaDon ct : DS_MON.values()) {
            if (ct.getThucUongId() == thucUongId) tong += ct.getSoLuong();
        }
        return tong;
    }

    public static long tongTien() {
        long tong = 0;
        for (ChiTietHoaDon ct : DS_MON.values()) tong += ct.getThanhTien();
        return tong;
    }

    public static boolean rong() {
        return DS_MON.isEmpty();
    }

    public static void xoaHet() {
        DS_MON.clear();
        datBanIdGoiY = 0;
    }
}
