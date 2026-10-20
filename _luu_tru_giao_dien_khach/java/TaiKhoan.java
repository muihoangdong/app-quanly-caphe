package hutech.cntt.app_quanlycafe.model;

public class TaiKhoan {
    public static final int KHACH_HANG = 0;
    public static final int NHAN_VIEN = 1;
    public static final int QUAN_TRI = 2;

    private int id;
    private String tenDangNhap;
    private String hoTen;
    private String soDienThoai;
    private String email;
    private int vaiTro;
    private String chucVu;
    private boolean dangHoatDong = true;
    private long ngayTao;

    public TaiKhoan() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTenDangNhap() { return tenDangNhap; }
    public void setTenDangNhap(String tenDangNhap) { this.tenDangNhap = tenDangNhap; }

    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }

    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getVaiTro() { return vaiTro; }
    public void setVaiTro(int vaiTro) { this.vaiTro = vaiTro; }

    public String getChucVu() { return chucVu; }
    public void setChucVu(String chucVu) { this.chucVu = chucVu; }

    public boolean isDangHoatDong() { return dangHoatDong; }
    public void setDangHoatDong(boolean dangHoatDong) { this.dangHoatDong = dangHoatDong; }

    public long getNgayTao() { return ngayTao; }
    public void setNgayTao(long ngayTao) { this.ngayTao = ngayTao; }

    public boolean laQuanTri() {
        return vaiTro == QUAN_TRI;
    }

    public String tenVaiTro() {
        if (vaiTro == QUAN_TRI) return "Quản trị viên";
        if (vaiTro == NHAN_VIEN) return "Nhân viên";
        return "Khách hàng";
    }
}
