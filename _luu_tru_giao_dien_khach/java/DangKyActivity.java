package hutech.cntt.app_quanlycafe;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.DieuHuong;
import hutech.cntt.app_quanlycafe.data.PhienDangNhap;
import hutech.cntt.app_quanlycafe.model.TaiKhoan;

/**
 * Khách tự đăng ký tài khoản bằng số điện thoại.
 */
public class DangKyActivity extends AppCompatActivity {

    EditText edtHoTen, edtSdt, edtEmail, edtMatKhau, edtNhapLai;
    CafeDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dang_ky);
        DieuHuong.apDungInsets(this);

        db = CafeDatabase.getInstance(this);
        ((TextView) findViewById(R.id.txtTieuDeManHinh)).setText("Đăng ký tài khoản");
        findViewById(R.id.btnQuayLai).setOnClickListener(v -> finish());

        edtHoTen = findViewById(R.id.edtHoTenDK);
        edtSdt = findViewById(R.id.edtSdtDK);
        edtEmail = findViewById(R.id.edtEmailDK);
        edtMatKhau = findViewById(R.id.edtMatKhauDK);
        edtNhapLai = findViewById(R.id.edtNhapLaiDK);

        findViewById(R.id.btnDangKy).setOnClickListener(v -> dangKy());
    }

    /** Trả về true nếu ô hợp lệ, ngược lại báo lỗi lên ô. */
    static boolean kiemTra(EditText edt, boolean dieuKien, String loi) {
        if (dieuKien) return true;
        edt.setError(loi);
        edt.requestFocus();
        return false;
    }

    static boolean laSoDienThoai(String sdt) {
        return sdt.matches("0\\d{9,10}");
    }

    private void dangKy() {
        String hoTen = edtHoTen.getText().toString().trim();
        String sdt = edtSdt.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String matKhau = edtMatKhau.getText().toString();
        String nhapLai = edtNhapLai.getText().toString();

        if (!kiemTra(edtHoTen, !hoTen.isEmpty(), "Nhập họ tên")) return;
        if (!kiemTra(edtSdt, laSoDienThoai(sdt), "Số điện thoại gồm 10-11 số, bắt đầu bằng 0")) return;
        if (!kiemTra(edtEmail, email.isEmpty() || Patterns.EMAIL_ADDRESS.matcher(email).matches(),
                "Email không hợp lệ")) return;
        if (!kiemTra(edtMatKhau, matKhau.length() >= 6, "Mật khẩu ít nhất 6 ký tự")) return;
        if (!kiemTra(edtNhapLai, nhapLai.equals(matKhau), "Mật khẩu nhập lại không khớp")) return;
        if (!kiemTra(edtSdt, !db.daTonTai(sdt, sdt, 0), "Số điện thoại đã được đăng ký")) return;

        TaiKhoan tk = new TaiKhoan();
        tk.setTenDangNhap(sdt);
        tk.setHoTen(hoTen);
        tk.setSoDienThoai(sdt);
        tk.setEmail(email);
        tk.setVaiTro(TaiKhoan.KHACH_HANG);
        tk.setId(db.themTaiKhoan(tk, matKhau));

        PhienDangNhap.luu(this, tk);
        Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
        PhienDangNhap.moManHinhChinh(this, tk);
    }
}
