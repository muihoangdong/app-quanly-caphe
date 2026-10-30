package hutech.cntt.app_quanlycafe.data;

import android.content.Context;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Tạo Retrofit gọi API PHP. Địa chỉ máy chủ (host:cổng) lưu trong máy, đổi được ở hộp thoại máy chủ.
 * Máy ảo: 10.0.2.2:8080 (là localhost của máy tính). Điện thoại cùng Wi-Fi: IP máy tính:8080.
 */
public class ApiClient {

    public static final String DIA_CHI_MAC_DINH = "10.0.2.2:8080";

    private static final String TEN_FILE = "cai_dat";
    private static final String KHOA_DIA_CHI = "dia_chi_api";

    /**
     * Các API PHP trong server/api: taikhoan.php, cauhinh.php, ban.php, thucdon.php, donhang.php.
     * Mọi API nhận POST JSON { "action": "...", ... } và trả ApiResponse.
     */
    public interface ApiService {
        @POST("{tep}")
        Call<ApiResponse> goi(@Path("tep") String tep, @Header("X-Token") String token, @Body JsonObject thamSo);
    }

    /** Kết quả chung của mọi API: { success, message, total, data }. */
    public static class ApiResponse {
        private boolean success;
        private String message;
        private int total;
        private JsonElement data;

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public int getTotal() { return total; }
        public JsonElement getData() { return data; }
    }

    /** Lỗi khi gọi API / dữ liệu: thông báo hiện thẳng cho người dùng. */
    public static class LoiCSDL extends Exception {
        private final int maHttp;

        public LoiCSDL(String thongBao) {
            this(thongBao, 0);
        }

        public LoiCSDL(String thongBao, int maHttp) {
            super(thongBao);
            this.maHttp = maHttp;
        }

        /** true khi token hết hạn hoặc tài khoản bị khóa (API trả 401): cần đăng nhập lại. */
        public boolean isHetPhien() {
            return maHttp == 401;
        }
    }

    private final Context context;
    private ApiService apiService;
    private String diaChiDangDung;

    public ApiClient(Context context) {
        this.context = context.getApplicationContext();
    }

    public String layDiaChi() {
        return context.getSharedPreferences(TEN_FILE, Context.MODE_PRIVATE)
                .getString(KHOA_DIA_CHI, DIA_CHI_MAC_DINH);
    }

    public void luuDiaChi(String diaChi) {
        String s = diaChi.trim().replace("http://", "").replace("https://", "");
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        if (!s.contains(":")) s += ":8080";
        context.getSharedPreferences(TEN_FILE, Context.MODE_PRIVATE).edit().putString(KHOA_DIA_CHI, s).apply();
    }

    /** Tạo lại Retrofit khi địa chỉ máy chủ thay đổi. */
    public synchronized ApiService getApiService() {
        String diaChi = layDiaChi();
        if (apiService == null || !diaChi.equals(diaChiDangDung)) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://" + diaChi + "/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            apiService = retrofit.create(ApiService.class);
            diaChiDangDung = diaChi;
        }
        return apiService;
    }
}
