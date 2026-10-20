package hutech.cntt.app_quanlycafe.model;

public class Ban {
    public static final int TRONG = 0;
    public static final int CO_KHACH = 1;
    public static final int CHO_THANH_TOAN = 2;
    public static final int DA_DAT = 3;
    public static final int BAO_TRI = 4;

    /** Giá trị cột `Tables`.status trong MySQL, theo đúng thứ tự hằng số ở trên. */
    private static final String[] MA_TRANG_THAI = {"EMPTY", "SERVING", "WAITING_PAYMENT", "RESERVED", "MAINTENANCE"};
    private static final String[] TEN_TRANG_THAI = {"Trống", "Đang phục vụ", "Chờ thanh toán", "Đã đặt", "Bảo trì"};

    public static int tuMa(String ma) {
        for (int i = 0; i < MA_TRANG_THAI.length; i++) {
            if (MA_TRANG_THAI[i].equals(ma)) return i;
        }
        return TRONG;
    }

    public static String maTrangThai(int trangThai) {
        return MA_TRANG_THAI[trangThai];
    }

    public static String tenTrangThai(int trangThai) {
        return TEN_TRANG_THAI[trangThai];
    }

    private int id;
    private String tenBan;
    private String khuVuc;
    private int soCho;
    private int trangThai;

    // Thông tin hóa đơn đang mở của bàn (nếu có)
    private int hoaDonId;
    private int soKhach;
    private long gioVao;
    private int soMon;
    private long tamTinh;

    public Ban() {
    }

    public Ban(int id, String tenBan, String khuVuc, int soCho, int trangThai) {
        this.id = id;
        this.tenBan = tenBan;
        this.khuVuc = khuVuc;
        this.soCho = soCho;
        this.trangThai = trangThai;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTenBan() { return tenBan; }
    public void setTenBan(String tenBan) { this.tenBan = tenBan; }

    public String getKhuVuc() { return khuVuc; }
    public void setKhuVuc(String khuVuc) { this.khuVuc = khuVuc; }

    public int getSoCho() { return soCho; }
    public void setSoCho(int soCho) { this.soCho = soCho; }

    public int getTrangThai() { return trangThai; }
    public void setTrangThai(int trangThai) { this.trangThai = trangThai; }

    public int getHoaDonId() { return hoaDonId; }
    public void setHoaDonId(int hoaDonId) { this.hoaDonId = hoaDonId; }

    public int getSoKhach() { return soKhach; }
    public void setSoKhach(int soKhach) { this.soKhach = soKhach; }

    public long getGioVao() { return gioVao; }
    public void setGioVao(long gioVao) { this.gioVao = gioVao; }

    public int getSoMon() { return soMon; }
    public void setSoMon(int soMon) { this.soMon = soMon; }

    public long getTamTinh() { return tamTinh; }
    public void setTamTinh(long tamTinh) { this.tamTinh = tamTinh; }

    /** Bàn chưa có order đang mở (trống, đã đặt hoặc bảo trì). */
    public boolean isTrong() {
        return hoaDonId == 0;
    }

    @Override
    public String toString() {
        return tenBan + " • " + khuVuc;
    }
}
