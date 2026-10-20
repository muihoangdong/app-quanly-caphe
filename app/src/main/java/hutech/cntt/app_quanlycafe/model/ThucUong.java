package hutech.cntt.app_quanlycafe.model;

public class ThucUong {
    private int id;
    private String maMon;
    private String tenMon;
    private int danhMucId;
    private String danhMuc;
    private String moTa;
    private long gia;
    private long giaVon;
    private String uriAnh;
    private boolean conBan;
    private boolean banChay;

    public ThucUong() {
        conBan = true;
    }

    public ThucUong(int id, String maMon, String tenMon, String danhMuc, String moTa,
                    long gia, long giaVon, String uriAnh, boolean conBan, boolean banChay) {
        this.id = id;
        this.maMon = maMon;
        this.tenMon = tenMon;
        this.danhMuc = danhMuc;
        this.moTa = moTa;
        this.gia = gia;
        this.giaVon = giaVon;
        this.uriAnh = uriAnh;
        this.conBan = conBan;
        this.banChay = banChay;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMaMon() { return maMon; }
    public void setMaMon(String maMon) { this.maMon = maMon; }

    public String getTenMon() { return tenMon; }
    public void setTenMon(String tenMon) { this.tenMon = tenMon; }

    public int getDanhMucId() { return danhMucId; }
    public void setDanhMucId(int danhMucId) { this.danhMucId = danhMucId; }

    public String getDanhMuc() { return danhMuc; }
    public void setDanhMuc(String danhMuc) { this.danhMuc = danhMuc; }

    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }

    public long getGia() { return gia; }
    public void setGia(long gia) { this.gia = gia; }

    public long getGiaVon() { return giaVon; }
    public void setGiaVon(long giaVon) { this.giaVon = giaVon; }

    public String getUriAnh() { return uriAnh; }
    public void setUriAnh(String uriAnh) { this.uriAnh = uriAnh; }

    public boolean isConBan() { return conBan; }
    public void setConBan(boolean conBan) { this.conBan = conBan; }

    public boolean isBanChay() { return banChay; }
    public void setBanChay(boolean banChay) { this.banChay = banChay; }
}
