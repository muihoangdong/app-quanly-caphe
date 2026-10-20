package hutech.cntt.app_quanlycafe;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.DieuHuong;
import hutech.cntt.app_quanlycafe.data.GiaoDienKhach;
import hutech.cntt.app_quanlycafe.data.PhienDangNhap;
import hutech.cntt.app_quanlycafe.data.TienIch;
import hutech.cntt.app_quanlycafe.model.DatBan;
import hutech.cntt.app_quanlycafe.model.DonOnline;
import hutech.cntt.app_quanlycafe.model.TaiKhoan;

/**
 * Khách: thông tin tài khoản, thống kê, sửa thông tin, đổi mật khẩu, đăng xuất.
 */
public class KhachTaiKhoanActivity extends AppCompatActivity {

    CafeDatabase db;
    TaiKhoan khach;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!PhienDangNhap.kiemTraQuyen(this, true, false, false)) return;
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_khach_tai_khoan);
        DieuHuong.apDungInsets(this);
        DieuHuong.ganThanhDieuHuongKhach(this, R.id.nav_k_tai_khoan);
        db = CafeDatabase.getInstance(this);

        findViewById(R.id.dongLichSuDatBan).setOnClickListener(v -> moDonCuaToi(false));
        findViewById(R.id.dongLichSuDon).setOnClickListener(v -> moDonCuaToi(true));
        findViewById(R.id.dongVeQuan).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(getString(R.string.app_name))
                .setMessage(getString(R.string.gio_mo_cua) + "\n" + getString(R.string.dia_chi_quan)
                        + "\n\nĐặt bàn trước qua app, đến giờ hẹn đưa mã đặt bàn cho nhân viên"
                        + " để được dẫn tới bàn. Bàn được giữ tối đa 30 phút.")
                .setPositiveButton("Đóng", null)
                .show());
        findViewById(R.id.dongThongTinCaNhan).setOnClickListener(v -> suaThongTin());
        findViewById(R.id.dongDoiMatKhau).setOnClickListener(v ->
                GiaoDienKhach.hienThiDoiMatKhau(this, khach.getId(), true));
        findViewById(R.id.dongDangXuat).setOnClickListener(v -> DieuHuong.xacNhanDangXuat(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        khach = PhienDangNhap.layTaiKhoan(this);
        if (khach != null) hienThi();
    }

    private void moDonCuaToi(boolean xemDonMon) {
        Intent in = new Intent(this, KhachDonCuaToiActivity.class);
        in.putExtra("xemDonMon", xemDonMon);
        in.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(in);
        overridePendingTransition(0, 0);
    }

    private void hienThi() {
        ((TextView) findViewById(R.id.txtAvatarTaiKhoan)).setText(GiaoDienKhach.chuCaiDau(khach.getHoTen()));
        ((TextView) findViewById(R.id.txtTenTaiKhoan)).setText(khach.getHoTen());
        String lienHe = khach.getSoDienThoai();
        if (khach.getEmail() != null && !khach.getEmail().isEmpty()) lienHe += " • " + khach.getEmail();
        ((TextView) findViewById(R.id.txtSdtTaiKhoan)).setText(lienHe);
        ((TextView) findViewById(R.id.txtThanhVienTu)).setText("Thành viên từ "
                + TienIch.ngay(khach.getNgayTao()).substring(3));

        int soLuotDat = 0;
        for (DatBan d : db.layDatBanCuaKhach(khach.getId())) {
            if (d.getTrangThai() != DatBan.DA_HUY) soLuotDat++;
        }
        int soDon = 0;
        long chiTieu = 0;
        for (DonOnline don : db.layDanhSachDonOnline(khach.getId(), -1)) {
            if (don.getTrangThai() == DonOnline.DA_HUY) continue;
            soDon++;
            if (don.isDaThanhToan()) chiTieu += don.getTongTien();
        }
        ((TextView) findViewById(R.id.txtSoLuotDat)).setText(String.valueOf(soLuotDat));
        ((TextView) findViewById(R.id.txtSoDonMon)).setText(String.valueOf(soDon));
        ((TextView) findViewById(R.id.txtTongChiTieu)).setText(chiTieu >= 1000
                ? TienIch.so(chiTieu / 1000) + "K" : TienIch.tien(chiTieu));
    }

    private void suaThongTin() {
        View view = getLayoutInflater().inflate(R.layout.dialog_k_thong_tin, null);
        EditText edtHoTen = view.findViewById(R.id.edtHoTenTT);
        EditText edtSdt = view.findViewById(R.id.edtSdtTT);
        EditText edtEmail = view.findViewById(R.id.edtEmailTT);
        edtHoTen.setText(khach.getHoTen());
        edtSdt.setText(khach.getSoDienThoai());
        edtEmail.setText(khach.getEmail());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Thông tin cá nhân")
                .setView(view)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String hoTen = edtHoTen.getText().toString().trim();
            String sdt = edtSdt.getText().toString().trim();
            String email = edtEmail.getText().toString().trim();
            if (!DangKyActivity.kiemTra(edtHoTen, !hoTen.isEmpty(), "Nhập họ tên")) return;
            if (!DangKyActivity.kiemTra(edtSdt, DangKyActivity.laSoDienThoai(sdt),
                    "Số điện thoại gồm 10-11 số, bắt đầu bằng 0")) return;
            if (!DangKyActivity.kiemTra(edtEmail, email.isEmpty()
                    || Patterns.EMAIL_ADDRESS.matcher(email).matches(), "Email không hợp lệ")) return;
            if (!DangKyActivity.kiemTra(edtSdt, !db.daTonTai(sdt, sdt, khach.getId()),
                    "Số điện thoại đã được dùng")) return;
            khach.setHoTen(hoTen);
            khach.setSoDienThoai(sdt);
            khach.setTenDangNhap(sdt);
            khach.setEmail(email);
            db.suaTaiKhoan(khach);
            PhienDangNhap.lamMoi(this);
            Toast.makeText(this, "Đã lưu thông tin", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            khach = PhienDangNhap.layTaiKhoan(this);
            hienThi();
        });
    }
}
