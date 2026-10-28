package hutech.cntt.app_quanlycafe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.ChayNen;
import hutech.cntt.app_quanlycafe.data.DieuHuong;


public class GiaoDienDangNhapActivity extends AppCompatActivity {

    EditText edtTenDangNhap, edtMatKhau;
    TextView txtLoi;
    MaterialButton btnDangNhap;
    View layoutForm;

    CafeDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_giao_dien_dang_nhap);
        DieuHuong.apDungInsets(this);

        db = CafeDatabase.getInstance(this);
        edtTenDangNhap = findViewById(R.id.edtTenDangNhap);
        edtMatKhau = findViewById(R.id.edtMatKhau);
        txtLoi = findViewById(R.id.txtLoiDangNhap);
        btnDangNhap = findViewById(R.id.btnDangNhap);
        layoutForm = findViewById(R.id.layoutFormDangNhap);

        btnDangNhap.setOnClickListener(v -> dangNhap());
        edtMatKhau.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) return false;
            dangNhap();
            return true;
        });

        if (db.daDangNhap()) kiemTraPhienCu();
    }

    private void kiemTraPhienCu() {
        layoutForm.setVisibility(View.INVISIBLE);
        ChayNen.chay(this, db::layNguoiDangNhap, nv -> {
            if (nv != null) {
                vaoTrangChu();
            } else {
                db.dangXuat();
                layoutForm.setVisibility(View.VISIBLE);
                hienThiLoi("Tài khoản đã bị khóa hoặc không còn tồn tại");
            }
        }, loi -> vaoTrangChu());
    }

    private void dangNhap() {
        String ten = edtTenDangNhap.getText().toString().trim();
        String matKhau = edtMatKhau.getText().toString();
        if (ten.isEmpty()) {
            edtTenDangNhap.setError("Nhập tên đăng nhập");
            return;
        }
        if (matKhau.isEmpty()) {
            edtMatKhau.setError("Nhập mật khẩu");
            return;
        }
        txtLoi.setVisibility(View.GONE);
        btnDangNhap.setEnabled(false);
        ChayNen.chay(this, () -> db.dangNhap(ten, matKhau), nv -> {
            btnDangNhap.setEnabled(true);
            edtMatKhau.setText("");
            if (CafeDatabase.MAT_KHAU_MAC_DINH.equals(matKhau)) {
                DieuHuong.hienThiDoiMatKhau(this, true, this::vaoTrangChu);
            } else {
                vaoTrangChu();
            }
        }, loi -> {
            btnDangNhap.setEnabled(true);
            hienThiLoi(loi);
        });
    }

    private void hienThiLoi(String loi) {
        boolean loiKetNoi = loi.toLowerCase().contains("kết nối");
        txtLoi.setText(loiKetNoi ? loi + "\nChạm vào đây để đổi địa chỉ máy chủ." : loi);
        txtLoi.setOnClickListener(loiKetNoi
                ? v -> DieuHuong.hienThiCaiDatMayChu(this, () -> txtLoi.setVisibility(View.GONE))
                : null);
        txtLoi.setClickable(loiKetNoi);
        txtLoi.setVisibility(View.VISIBLE);
    }

    private void vaoTrangChu() {
        startActivity(new Intent(this, GiaoDienTrangChuActivity.class));
        overridePendingTransition(R.anim.tab_vao, R.anim.tab_ra);
        finish();
    }
}
