package hutech.cntt.app_quanlycafe.model;

/** Một lượt đặt bàn trước của khách qua app. */
public class DatBan {
    public static final int CHO_DEN = 0;
    public static final int DA_NHAN_BAN = 1;
    public static final int DA_HUY = 2;
    public static final int KHONG_DEN = 3;

    /** Mỗi lượt đặt giữ bàn trong 2 tiếng. */
    public static final long THOI_LUONG_GIU_BAN = 2 * 60 * 60 * 1000L;
    /** Quá giờ hẹn 30 phút mà khách chưa đến thì coi như không đến. */
    public static final long THOI_GIAN_CHO = 30 * 60 * 1000L;

    private int id;
    private String maDat;
    private int khachId;
    private String tenKhach;
    private String soDienThoai;
    private int banId;
    private String tenBan;
    private String khuVuc;
    private int soKhach;
    private long thoiGian;
    private String ghiChu;
    private int trangThai;
    private long ngayTao;
    private int hoaDonId;

    public DatBan() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMaDat() { return maDat; }
    public void setMaDat(String maDat) { this.maDat = maDat; }

    public int getKhachId() { return khachId; }
    public void setKhachId(int khachId) { this.khachId = khachId; }

    public String getTenKhach() { return tenKhach; }
    public void setTenKhach(String tenKhach) { this.tenKhach = tenKhach; }

    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }

    public int getBanId() { return banId; }
    public void setBanId(int banId) { this.banId = banId; }

    public String getTenBan() { return tenBan; }
    public void setTenBan(String tenBan) { this.tenBan = tenBan; }

    public String getKhuVuc() { return khuVuc; }
    public void setKhuVuc(String khuVuc) { this.khuVuc = khuVuc; }

    public int getSoKhach() { return soKhach; }
    public void setSoKhach(int soKhach) { this.soKhach = soKhach; }

    public long getThoiGian() { return thoiGian; }
    public void setThoiGian(long thoiGian) { this.thoiGian = thoiGian; }

    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    public int getTrangThai() { return trangThai; }
    public void setTrangThai(int trangThai) { this.trangThai = trangThai; }

    public long getNgayTao() { return ngayTao; }
    public void setNgayTao(long ngayTao) { this.ngayTao = ngayTao; }

    public int getHoaDonId() { return hoaDonId; }
    public void setHoaDonId(int hoaDonId) { this.hoaDonId = hoaDonId; }

    public String tenTrangThai() {
        switch (trangThai) {
            case DA_NHAN_BAN: return "Đã nhận bàn";
            case DA_HUY: return "Đã hủy";
            case KHONG_DEN: return "Không đến";
            default: return "Chờ đến";
        }
    }

    @Override
    public String toString() {
        return "#" + maDat + " • " + tenBan + " • " + khuVuc;
    }
}
