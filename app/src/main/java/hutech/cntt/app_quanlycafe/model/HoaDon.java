package hutech.cntt.app_quanlycafe.model;

public class HoaDon {
    public static final int DANG_PHUC_VU = 0;
    public static final int DA_THANH_TOAN = 1;
    public static final int DA_HUY = 2;

    /** Phương thức thanh toán: mã lưu trong MySQL (Invoices.payment_method) và tên hiển thị. */
    public static final String[] MA_PHUONG_THUC = {"BANK_QR", "CASH", "CARD", "E_WALLET"};
    public static final String[] TEN_PHUONG_THUC = {"VietQR", "Tiền mặt", "Quẹt POS", "Ví MoMo/Zalo"};

    public static String tenPhuongThuc(String ma) {
        for (int i = 0; i < MA_PHUONG_THUC.length; i++) {
            if (MA_PHUONG_THUC[i].equals(ma)) return TEN_PHUONG_THUC[i];
        }
        return ma;
    }

    public static String maPhuongThuc(String ten) {
        for (int i = 0; i < TEN_PHUONG_THUC.length; i++) {
            if (TEN_PHUONG_THUC[i].equals(ten)) return MA_PHUONG_THUC[i];
        }
        return ten;
    }

    private int id;
    private String maHoaDon;
    private int banId;
    private String tenBan;
    private String khuVuc;
    private int soKhach;
    private long gioVao;
    private long gioRa;
    private int trangThai;
    private long tamTinh;
    private long giamGia;
    private long vat;
    private long tongTien;
    private String phuongThuc;
    private long khachDua;
    private int soMon;

    public HoaDon() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMaHoaDon() { return maHoaDon; }
    public void setMaHoaDon(String maHoaDon) { this.maHoaDon = maHoaDon; }

    public int getBanId() { return banId; }
    public void setBanId(int banId) { this.banId = banId; }

    public String getTenBan() { return tenBan; }
    public void setTenBan(String tenBan) { this.tenBan = tenBan; }

    public String getKhuVuc() { return khuVuc; }
    public void setKhuVuc(String khuVuc) { this.khuVuc = khuVuc; }

    public int getSoKhach() { return soKhach; }
    public void setSoKhach(int soKhach) { this.soKhach = soKhach; }

    public long getGioVao() { return gioVao; }
    public void setGioVao(long gioVao) { this.gioVao = gioVao; }

    public long getGioRa() { return gioRa; }
    public void setGioRa(long gioRa) { this.gioRa = gioRa; }

    public int getTrangThai() { return trangThai; }
    public void setTrangThai(int trangThai) { this.trangThai = trangThai; }

    public long getTamTinh() { return tamTinh; }
    public void setTamTinh(long tamTinh) { this.tamTinh = tamTinh; }

    public long getGiamGia() { return giamGia; }
    public void setGiamGia(long giamGia) { this.giamGia = giamGia; }

    public long getVat() { return vat; }
    public void setVat(long vat) { this.vat = vat; }

    public long getTongTien() { return tongTien; }
    public void setTongTien(long tongTien) { this.tongTien = tongTien; }

    public String getPhuongThuc() { return phuongThuc; }
    public void setPhuongThuc(String phuongThuc) { this.phuongThuc = phuongThuc; }

    public long getKhachDua() { return khachDua; }
    public void setKhachDua(long khachDua) { this.khachDua = khachDua; }

    public int getSoMon() { return soMon; }
    public void setSoMon(int soMon) { this.soMon = soMon; }
}
