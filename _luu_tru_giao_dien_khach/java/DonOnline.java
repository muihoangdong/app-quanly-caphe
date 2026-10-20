package hutech.cntt.app_quanlycafe.model;

/** Đơn gọi món khách đặt qua app (dùng tại quán theo lượt đặt bàn hoặc mang đi). */
public class DonOnline {
    public static final int CHO_XAC_NHAN = 0;
    public static final int DANG_PHA_CHE = 1;
    public static final int HOAN_THANH = 2;
    public static final int DA_HUY = 3;

    public static final String TAI_QUAN = "Dùng tại quán";
    public static final String MANG_DI = "Mang đi";
    public static final String TRA_TAI_QUAY = "Trả tại quầy";

    private int id;
    private String maDon;
    private int khachId;
    private String tenKhach;
    private String soDienThoai;
    private int datBanId;
    private String thongTinBan;
    private String hinhThuc;
    private String phuongThuc;
    private boolean daThanhToan;
    private int trangThai;
    private long tongTien;
    private String ghiChu;
    private long thoiGian;
    private int soMon;

    public DonOnline() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMaDon() { return maDon; }
    public void setMaDon(String maDon) { this.maDon = maDon; }

    public int getKhachId() { return khachId; }
    public void setKhachId(int khachId) { this.khachId = khachId; }

    public String getTenKhach() { return tenKhach; }
    public void setTenKhach(String tenKhach) { this.tenKhach = tenKhach; }

    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }

    public int getDatBanId() { return datBanId; }
    public void setDatBanId(int datBanId) { this.datBanId = datBanId; }

    public String getThongTinBan() { return thongTinBan; }
    public void setThongTinBan(String thongTinBan) { this.thongTinBan = thongTinBan; }

    public String getHinhThuc() { return hinhThuc; }
    public void setHinhThuc(String hinhThuc) { this.hinhThuc = hinhThuc; }

    public String getPhuongThuc() { return phuongThuc; }
    public void setPhuongThuc(String phuongThuc) { this.phuongThuc = phuongThuc; }

    public boolean isDaThanhToan() { return daThanhToan; }
    public void setDaThanhToan(boolean daThanhToan) { this.daThanhToan = daThanhToan; }

    public int getTrangThai() { return trangThai; }
    public void setTrangThai(int trangThai) { this.trangThai = trangThai; }

    public long getTongTien() { return tongTien; }
    public void setTongTien(long tongTien) { this.tongTien = tongTien; }

    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    public long getThoiGian() { return thoiGian; }
    public void setThoiGian(long thoiGian) { this.thoiGian = thoiGian; }

    public int getSoMon() { return soMon; }
    public void setSoMon(int soMon) { this.soMon = soMon; }

    public String tenTrangThai() {
        switch (trangThai) {
            case DANG_PHA_CHE: return "Đang pha chế";
            case HOAN_THANH: return "Hoàn thành";
            case DA_HUY: return "Đã hủy";
            default: return "Chờ xác nhận";
        }
    }
}
