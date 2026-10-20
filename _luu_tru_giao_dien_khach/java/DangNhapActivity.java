package hutech.cntt.app_quanlycafe;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.DieuHuong;
import hutech.cntt.app_quanlycafe.data.PhienDangNhap;
import hutech.cntt.app_quanlycafe.model.TaiKhoan;

/**
 * Màn hình khởi động: đăng nhập cho khách hàng hoặc nhân viên / quản trị viên.
 */
public class DangNhapActivity extends AppCompatActivity {

    RadioGroup rgLoaiTaiKhoan;
    EditText edtTaiKhoan, edtMatKhau;
    TextView txtNhanTaiKhoan, txtDangKy, txtGoiY;
    MaterialButton btnDangNhap;

    CafeDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Đã đăng nhập từ lần trước thì vào thẳng màn hình chính
        TaiKhoan daDangNhap = PhienDangNhap.layTaiKhoan(this);
        if (daDangNhap != null) {
            PhienDangNhap.moManHinhChinh(this, daDangNhap);
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dang_nhap);
        DieuHuong.apDungInsets(this);

        db = CafeDatabase.getInstance(this);
        rgLoaiTaiKhoan = findViewById(R.id.rgLoaiTaiKhoan);
        edtTaiKhoan = findViewById(R.id.edtTaiKhoan);
        edtMatKhau = findViewById(R.id.edtMatKhau);
        txtNhanTaiKhoan = findViewById(R.id.txtNhanTaiKhoan);
        txtDangKy = findViewById(R.id.txtDangKy);
        txtGoiY = findViewById(R.id.txtGoiYTaiKhoan);
        btnDangNhap = findViewById(R.id.btnDangNhap);

        rgLoaiTaiKhoan.setOnCheckedChangeListener((group, checkedId) -> capNhatCheDo());
        capNhatCheDo();

        CheckBox chkHien = findViewById(R.id.chkHienMatKhau);
        chkHien.setOnCheckedChangeListener((b, hien) -> {
            edtMatKhau.setInputType(InputType.TYPE_CLASS_TEXT | (hien
                    ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_TEXT_VARIATION_PASSWORD));
            edtMatKhau.setSelection(edtMatKhau.length());
        });

        btnDangNhap.setOnClickListener(v -> dangNhap());
        txtDangKy.setOnClickListener(v -> startActivity(new Intent(this, DangKyActivity.class)));
    }

    private boolean laNhanVien() {
        return rgLoaiTaiKhoan.getCheckedRadioButtonId() == R.id.radNhanVien;
    }

    private void capNhatCheDo() {
        edtTaiKhoan.setError(null);
        edtMatKhau.setError(null);
        if (laNhanVien()) {
            txtNhanTaiKhoan.setText("Tên đăng nhập");
            edtTaiKhoan.setHint("VD: admin");
            edtTaiKhoan.setInputType(InputType.TYPE_CLASS_TEXT);
            edtTaiKhoan.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_person, 0, 0, 0);
            txtDangKy.setVisibility(View.GONE);
            btnDangNhap.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.espresso)));
            txtGoiY.setText("Tài khoản dùng thử\n• Quản trị: admin / admin123\n• Nhân viên: nhanvien / 123456"
                    + "\nTài khoản nhân viên do quản trị viên cấp trong mục Nhân viên.");
        } else {
            txtNhanTaiKhoan.setText("Số điện thoại");
            edtTaiKhoan.setHint("VD: 0909123456");
            edtTaiKhoan.setInputType(InputType.TYPE_CLASS_PHONE);
            edtTaiKhoan.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_phone, 0, 0, 0);
            txtDangKy.setVisibility(View.VISIBLE);
            btnDangNhap.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.tch_cam)));
            txtGoiY.setText("Tài khoản khách dùng thử: 0909123456 / 123456");
        }
    }

    private void dangNhap() {
        String taiKhoan = edtTaiKhoan.getText().toString().trim();
        String matKhau = edtMatKhau.getText().toString();
        if (taiKhoan.isEmpty()) {
            edtTaiKhoan.setError(laNhanVien() ? "Nhập tên đăng nhập" : "Nhập số điện thoại");
            edtTaiKhoan.requestFocus();
            return;
        }
        if (matKhau.isEmpty()) {
            edtMatKhau.setError("Nhập mật khẩu");
            edtMatKhau.requestFocus();
            return;
        }
        TaiKhoan tk = db.dangNhap(taiKhoan, matKhau, laNhanVien());
        if (tk == null) {
            Toast.makeText(this, "Sai tài khoản hoặc mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!tk.isDangHoatDong()) {
            Toast.makeText(this, "Tài khoản đã bị khóa, liên hệ quản lý quán", Toast.LENGTH_LONG).show();
            return;
        }
        PhienDangNhap.luu(this, tk);
        Toast.makeText(this, "Xin chào " + tk.getHoTen(), Toast.LENGTH_SHORT).show();
        PhienDangNhap.moManHinhChinh(this, tk);
    }
}
