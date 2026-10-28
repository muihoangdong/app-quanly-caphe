package hutech.cntt.app_quanlycafe.model;

/** Nhân viên / quản lý (bảng Users). */
public class NhanVien {
    private final int id;
    private final String tenDangNhap;
    private final String hoTen;
    private final boolean quanLy;
    private final boolean hoatDong; // false = tài khoản đã bị khóa

    public NhanVien(int id, String tenDangNhap, String hoTen, boolean quanLy, boolean hoatDong) {
        this.id = id;
        this.tenDangNhap = tenDangNhap;
        this.hoTen = hoTen;
        this.quanLy = quanLy;
        this.hoatDong = hoatDong;
    }

    public int getId() { return id; }
    public String getTenDangNhap() { return tenDangNhap; }
    public String getHoTen() { return hoTen; }
    public boolean isQuanLy() { return quanLy; }
    public boolean isHoatDong() { return hoatDong; }

    public String getVaiTro() {
        return quanLy ? "Quản lý" : "Nhân viên";
    }

    @Override
    public String toString() {
        return hoTen + " • " + getVaiTro();
    }
}
