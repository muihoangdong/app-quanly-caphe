package hutech.cntt.app_quanlycafe.model;

public class DanhMuc {
    private int id;
    private String ten;
    private int soMon;

    public DanhMuc(int id, String ten, int soMon) {
        this.id = id;
        this.ten = ten;
        this.soMon = soMon;
    }

    public int getId() { return id; }

    public String getTen() { return ten; }

    public int getSoMon() { return soMon; }

    @Override
    public String toString() {
        return ten;
    }
}
