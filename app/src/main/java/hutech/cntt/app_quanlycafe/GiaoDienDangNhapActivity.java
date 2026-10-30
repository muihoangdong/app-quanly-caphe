package hutech.cntt.app_quanlycafe;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.CafeDatabase.ChayNen;
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

        // Android 17+ (API 37): app phải được cấp quyền mạng cục bộ mới gọi được API trên máy tính
        // (10.0.2.2 của máy ảo / IP máy tính trong Wi-Fi)
        if (Build.VERSION.SDK_INT >= 37
                && checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED) {
            xinQuyenMangCucBo.launch(Manifest.permission.ACCESS_LOCAL_NETWORK);
        } else {
            batDau();
        }
    }

    private final ActivityResultLauncher<String> xinQuyenMangCucBo =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), duocCap -> {
                if (duocCap) {
                    batDau();
                } else {
                    hienThiLoi("Chưa cấp quyền \"Thiết bị ở gần / mạng cục bộ\" nên app không kết nối được máy chủ."
                            + " Hãy cấp quyền trong Cài đặt của Android rồi mở lại app.");
                }
            });

    private void batDau() {
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
