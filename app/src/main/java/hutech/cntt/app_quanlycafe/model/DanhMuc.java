package hutech.cntt.app_quanlycafe.model;

public class DanhMuc {
    private int id;
    private String ten;
    private int soMon;

    public DanhMuc(int soMon, String ten, int id) {
        this.soMon = soMon;
        this.ten = ten;
        this.id = id;
    }

    public int getId() { return id; }

    public String getTen() { return ten; }

    public int getSoMon() { return soMon; }

    @Override
    public String toString() {
        return ten;
    }
}
