package hutech.cntt.app_quanlycafe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.DieuHuong;


public class GiaoDienCaiDatActivity extends AppCompatActivity {

    CafeDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_giao_dien_cai_dat);
        DieuHuong.apDungInsets(this);
        DieuHuong.ganThanhDieuHuong(this, R.id.nav_cai_dat);

        db = CafeDatabase.getInstance(this);

        boolean quanLy = db.laQuanLy();
        ganThe(R.id.dongThucDon, R.drawable.ic_cup, "Thực đơn", "Quản lý món, danh mục",
                v -> startActivity(new Intent(this, GiaoDienQuanLyThucDonActivity.class)));
        ganThe(R.id.dongTaiKhoan, R.drawable.ic_people, "Tài khoản nhân viên",
                "Thêm nhân viên, đặt lại mật khẩu, khóa tài khoản",
                v -> startActivity(new Intent(this, GiaoDienTaiKhoanActivity.class)));
        findViewById(R.id.dongThucDon).setVisibility(quanLy ? View.VISIBLE : View.GONE);
        findViewById(R.id.dongTaiKhoan).setVisibility(quanLy ? View.VISIBLE : View.GONE);

        ganThe(R.id.dongLichSuHoaDon, R.drawable.ic_receipt, "Lịch sử hóa đơn",
                "Xem các hóa đơn đã thanh toán",
                v -> startActivity(new Intent(this, GiaoDienLichSuHoaDonActivity.class)));
        ganThe(R.id.dongDoiMatKhau, R.drawable.ic_lock, "Đổi mật khẩu", "Mật khẩu đăng nhập của bạn",
                v -> DieuHuong.hienThiDoiMatKhau(this, false, null));
        ganThe(R.id.dongDangXuat, R.drawable.ic_logout, "Đăng xuất", "Kết thúc ca, đổi người đăng nhập",
                v -> DieuHuong.xacNhanDangXuat(this));
    }

    private void ganThe(int id, int icon, String ten, String moTa, View.OnClickListener xuLy) {
        View the = findViewById(id);
        ((ImageView) the.findViewById(R.id.imgTheQuanLy)).setImageResource(icon);
        ((TextView) the.findViewById(R.id.txtTenTheQuanLy)).setText(ten);
        ((TextView) the.findViewById(R.id.txtMoTaTheQuanLy)).setText(moTa);
        the.setOnClickListener(xuLy);
    }
}
