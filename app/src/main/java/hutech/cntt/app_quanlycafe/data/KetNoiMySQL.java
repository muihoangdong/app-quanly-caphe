package hutech.cntt.app_quanlycafe.data;

import android.content.Context;
import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class KetNoiMySQL {

    public static final String DIA_CHI_MAC_DINH = "127.0.0.1:3307";

    private static final String TEN_CSDL = "quanlycafe";
    // Tài khoản chỉ có quyền SELECT / INSERT / UPDATE / DELETE trên CSDL quanlycafe
    private static final String TAI_KHOAN = "cafe";
    private static final String MAT_KHAU = "cafe123";

    private static final String TEN_FILE = "cai_dat";
    private static final String KHOA_DIA_CHI = "dia_chi_mysql";

    public static class LoiCSDL extends Exception {
        public LoiCSDL(String thongBao) {
            super(thongBao);
        }
    }

    static {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Thiếu thư viện mysql-connector-java", e);
        }
    }

    private final Context context;

    public KetNoiMySQL(Context context) {
        this.context = context.getApplicationContext();
    }

    public String layDiaChi() {
        return context.getSharedPreferences(TEN_FILE, Context.MODE_PRIVATE)
                .getString(KHOA_DIA_CHI, DIA_CHI_MAC_DINH);
    }

    public void luuDiaChi(String diaChi) {
        String s = diaChi.trim().replace("http://", "").replace("jdbc:mysql://", "");
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        if (!s.contains(":")) s += ":3307";
        context.getSharedPreferences(TEN_FILE, Context.MODE_PRIVATE).edit().putString(KHOA_DIA_CHI, s).apply();
    }

    public Connection moKetNoi() throws LoiCSDL {
        String url = "jdbc:mysql://" + layDiaChi() + "/" + TEN_CSDL
                + "?useUnicode=true&characterEncoding=UTF-8&useSSL=false"
                + "&connectTimeout=5000&socketTimeout=15000";
        try {
            return DriverManager.getConnection(url, TAI_KHOAN, MAT_KHAU);
        } catch (SQLException e) {
            Log.w("KetNoiMySQL", "Không kết nối được " + url, e);
            throw new LoiCSDL("Không kết nối được MySQL " + layDiaChi()
                    + ".\nKiểm tra Docker (MySQL) đã chạy và địa chỉ máy chủ còn đúng.");
        }
    }

    public static LoiCSDL dichLoi(SQLException e) {
        Log.w("KetNoiMySQL", "Lỗi SQL " + e.getErrorCode(), e);
        switch (e.getErrorCode()) {
            case 1062:
                return new LoiCSDL("Tên đã tồn tại, hãy chọn tên khác");
            case 1451:
                return new LoiCSDL("Dữ liệu đang được dùng ở nơi khác nên không xóa được");
            case 1452:
                return new LoiCSDL("Dữ liệu liên kết không tồn tại");
            default:
                if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                    return new LoiCSDL("Mất kết nối tới MySQL, hãy thử lại");
                }
                return new LoiCSDL("Lỗi CSDL: " + e.getMessage());
        }
    }
}
