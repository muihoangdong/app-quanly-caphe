package hutech.cntt.app_quanlycafe.model;

public class ChiTietHoaDon {
    public static final int DANG_PHA_CHE = 0;
    public static final int DA_LEN_MON = 1;

    private int id;
    private int hoaDonId;
    private int thucUongId;
    private String tenMon;
    private String danhMuc;
    private String uriAnh;
    private long donGia;
    private int soLuong;
    private String ghiChu;
    private int trangThai;
    private long thoiGian;

    public ChiTietHoaDon() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getHoaDonId() { return hoaDonId; }
    public void setHoaDonId(int hoaDonId) { this.hoaDonId = hoaDonId; }

    public int getThucUongId() { return thucUongId; }
    public void setThucUongId(int thucUongId) { this.thucUongId = thucUongId; }

    public String getTenMon() { return tenMon; }
    public void setTenMon(String tenMon) { this.tenMon = tenMon; }

    public String getDanhMuc() { return danhMuc; }
    public void setDanhMuc(String danhMuc) { this.danhMuc = danhMuc; }

    public String getUriAnh() { return uriAnh; }
    public void setUriAnh(String uriAnh) { this.uriAnh = uriAnh; }

    public long getDonGia() { return donGia; }
    public void setDonGia(long donGia) { this.donGia = donGia; }

    public int getSoLuong() { return soLuong; }
    public void setSoLuong(int soLuong) { this.soLuong = soLuong; }

    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    public int getTrangThai() { return trangThai; }
    public void setTrangThai(int trangThai) { this.trangThai = trangThai; }

    public long getThoiGian() { return thoiGian; }
    public void setThoiGian(long thoiGian) { this.thoiGian = thoiGian; }

    public long getThanhTien() {
        return donGia * soLuong;
    }
}
